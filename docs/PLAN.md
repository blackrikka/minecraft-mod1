# 산업모드 설계안 (modid: `ironworks`)

역할 분담: **설계·검증 = Sonnet 5.5 / 구현 = Haiku 5.5**. 구현 담당은 이 문서의 클래스명·규약을 그대로 따른다.

## 1. 기술 스택
- NeoForge 1.21.1 (`21.1.x`, 빌드 시점에 최신 21.1 확정) / Java 21 / Gradle 8.x + ModDevGradle
- 패키지: `com.blackrikka.ironworks`
- 에너지: NeoForge `Capabilities.EnergyStorage.BLOCK` (FE). 케이블 네트워크는 M1.5에서 단순 BFS 방식.
- 데이터: 레시피/로트테이블/블록스테이트/언어는 **수동 JSON**(datagen 생략, 구현 난이도 낮춤). 언어는 `en_us`, `ko_kr`.
- 텍스처: 임시 16x16 단색 PNG를 `tools/gen_textures.py`로 생성.

## 2. 디렉터리 구조
```
build.gradle / settings.gradle / gradle.properties
src/main/java/com/blackrikka/ironworks/
  Ironworks.java                 # @Mod 진입점, 레지스트리 등록
  registry/ModBlocks.java ModItems.java ModBlockEntities.java ModMenus.java ModCreativeTabs.java ModRecipes.java
  energy/ (M1) EnergyBlockEntity.java  CableBlock.java  CableBlockEntity.java
  machine/ (M1) MachineBlockEntity.java(추상) GeneratorBlock(+BE) CrusherBlock(+BE) FurnaceMachine... MachineMenu MachineScreen
  conveyor/ (M2) ConveyorBlock.java ConveyorBlockEntity.java SplitterBlock.java FilterItem...
  storage/ (M3) ServerRackBlock(+BE) DriveItem DriveData(DataComponent) NetworkCableBlock
              DbTerminalBlock(+BE) DbQuery.java DbMenu DbScreen
src/main/resources/assets/ironworks/{blockstates,models,textures,lang}
src/main/resources/data/ironworks/{recipe,loot_table}
tools/gen_textures.py
```

## 3. 마일스톤

### M0 – 골격
- Gradle/ModDevGradle 설정, `Ironworks` 진입점, 크리에이티브 탭 1개, 테스트 블록 1개.
- 완료 기준: `./gradlew build` 성공, `runClient`에서 블록 확인.

### M1 – 에너지 + 기계
- `EnergyStorage` 기반 `MachineBlockEntity`(진행도 `progress`, 서버 틱에서만 처리, `setChanged`/`sendBlockUpdated`, NBT 저장).
- 발전기(연료→FE), 분쇄기(광석→가루 x2), 전기 용광로, 압축기. 각 기계: 입력1/출력1 슬롯 + 에너지 바 GUI(`MachineMenu`/`MachineScreen`).
- 케이블: 인접 에너지 캡 간 전송(블록 틱에서 이웃에 `receiveEnergy`). 전송률 상한 지정.
- 완료 기준: 발전기→케이블→분쇄기 연결로 광석 가공.

### M1b – 에너지 호환 + 원시 발전기 (확정)
**호환 원칙**: 자체 에너지 API를 만들지 않는다. 단위는 **FE**이고 NeoForge `Capabilities.EnergyStorage.BLOCK`(`IEnergyStorage`)만 사용한다. Mekanism·Thermal·Create 등 대부분의 모드가 같은 표준이므로, 케이블/기계가 서로 바로 연결된다.
- `RegisterCapabilitiesEvent`에서 BE 타입별로 `EnergyStorage.BLOCK` 등록(면별 노출 제어: 발전기는 모든 면 **추출만** 허용, `canReceive=false`).
- 이웃 푸시: 서버 틱에서 6방향 이웃을 `level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(d), d.getOpposite())`로 조회해 `receiveEnergy`. 조회 결과는 캐시하지 않고 이웃 변경 시에만 재조회(성능).
- 공용 베이스 `energy/ModEnergyStorage`(`EnergyStorage` 상속, 변경 시 `setChanged` 콜백, 내부용 `setEnergy`)와 툴팁 유틸(`1,234 / 10,000 FE`).

**원시 발전기 `primitive_generator`**
- 연료: 바닐라 연료 전부(`CommonHooks.getBurnTime(stack, RecipeType.SMELTING)`, 모드 연료 포함). 슬롯 1칸.
- 출력: 10 FE/tick 생성(석탄 1개 = 1600틱 = 16,000 FE). 내부 버퍼 10,000 FE, 면당 최대 푸시 40 FE/tick.
- **버퍼가 가득 차면 연료를 소비하지 않고 멈춘다**(생성량이 버퍼 여유보다 클 때 정지).
- 연료 아이템에 남는 용기(예: 용암 양동이의 빈 양동이)는 `getCraftingRemainingItem`으로 처리.
- 블록상태 `lit`(+ `facing`), 켜지면 광원 레벨 13, 연기 파티클(클라이언트 `animateTick`).
- `ItemHandler` capability: 연료만 삽입 가능, 추출 불가(호퍼·컨베이어 연동).
- GUI(`PrimitiveGeneratorMenu`/`Screen`): 연료 슬롯 + 인벤토리 + 불꽃 게이지(남은 연소) + 에너지 바(마우스오버 FE 표시). 데이터 동기화는 `ContainerData`.
- 저장: 인벤토리, `energy`, `burnTime`, `burnTotal`을 NBT에 저장(1.21.1 시그니처 `saveAdditional(CompoundTag, HolderLookup.Provider)`).
- 레시피(작업대): 돌 5 + 화로 1 + 철 주괴 1 (바닐라 재료만, 첫 기계 원칙). 모든 기계 레시피의 기반이 되는 `machine_casing` 은 M1c.
- 완료 기준: `build` 성공, `runServer` 오류 없음, 발전기에 석탄을 넣으면 FE가 쌓이고 인접한 타 모드(또는 테스트용 배터리 블록)로 전송됨, 월드 재접속 후 상태 유지.
- 테스트용 `creative_battery`(무한 소비 싱크, 수신만 허용)를 개발용으로 함께 추가해 전송을 검증한다.

### M1c – 전용 케이블 (확정)
- 블록 `copper_cable`(+BE). 6면 연결 상태(`north/east/south/west/up/down` 불리언), 멀티파트 블록스테이트(중앙 코어 + 면별 팔), 연결 상태에 따른 `VoxelShape`.
- 연결 대상: 다른 케이블, 또는 해당 면에 `Capabilities.EnergyStorage.BLOCK`을 노출하는 모든 블록(타 모드 포함). 연결 갱신은 `getStateForPlacement`, `onPlace`, `neighborChanged`(Level)에서 재계산(`updateShape`에서는 capability 조회 금지).
- 전송 모델(네트워크 매니저 없이 케이블 단위): 케이블마다 내부 버퍼 512 FE, 면당 최대 128 FE/tick.
  - 수신: 모든 면에서 `receiveEnergy` 허용(버퍼 한도 내).
  - 분배(서버 틱): 인접 **비케이블** 수신자에게 라운드로빈으로 푸시. 인접 **케이블**에는 `자기 버퍼 > 상대 버퍼`일 때만 차이의 절반까지 푸시(되돌아 흐르는 핑퐁 방지).
  - 인접 비케이블 중 `canExtract()`이며 발전/저장 성격인 블록에서 당겨오기(pull)는 이번 단계에서는 하지 않는다(발전기가 푸시하므로 충분).
- 레시피: 고무(위·아래 각 3) + 구리 주괴 3(가운데) → 케이블 6개. 고무로 절연한다는 설정.
- 성능: 버퍼 0이면 틱 스킵, 연결 변경 시에만 이웃 목록 재조회.
- 완료 기준: 발전기 → 케이블 여러 칸 → 배터리/기계 전송, 케이블 분기, 월드 재접속 후 유지, `runServer` 오류 없음.

### 남은 작업 큐 (순차 진행: 각 단계 구현 → 빌드/서버 검증 → 커밋)
1. **M1d** 기계 프레임워크 + 분쇄기 + 전기 용광로 + 주석 광석/가루/주괴/청동 + `machine_casing`
2. **M1e** 합금로(청동·강철), 압축기(판), 플라스틱, 회로 조립기(기초 회로), 실리콘 광석/웨이퍼/고급 회로
3. **M2** 컨베이어(+분배기, 필터)
4. **M3a** 드라이브 아이템 + 서버랙 + 네트워크 케이블
5. **M3b** DB 터미널(검색·정렬·페이지·넣기/꺼내기, 패킷)
6. **M4** 업적 트리(바닐라→모드 진행 안내), 밸런스, 정리

### M1d – 기계 프레임워크 (확정)
- **레시피 타입**: `ironworks:crushing`(후속: `alloying`, `compressing`, `circuit_assembly`)을 하나의 레시피 클래스 `MachineRecipe`로 구현(레시피 타입/시리얼라이저만 다르게 등록). JSON: `{"type":"ironworks:crushing","ingredients":[{"ingredient":{"item":"minecraft:iron_ore"},"count":1}],"result":{"id":"ironworks:iron_dust","count":2},"time":100,"energy_per_tick":20}`. 재료 슬롯 수는 1~4. 입력은 `ingredients` 순서와 무관하게 매칭.
- **`MachineBlockEntity`(추상)**: 입력 슬롯 N개 + 출력 슬롯 1개(`ItemStackHandler`), `ModEnergyStorage`(버퍼 10,000, 수신 128/틱, 외부 추출 불가), `progress`/`maxProgress`. 서버 틱: 레시피 매칭 → 출력 가능 → 전력 충분하면 진행(틱당 `energy_per_tick` 소비), 전력 부족이면 진행 정지(감소 없음), 완료 시 입력 소비·출력 삽입. 캡: ItemHandler(입력 슬롯 삽입만, 출력 슬롯 추출만), EnergyStorage(수신만). `lit` 블록상태.
- **GUI**: 공용 `MachineMenu`/`MachineScreen`(`GuiGraphics.fill` 기반): 입력 슬롯 개수에 따라 슬롯 배치, 진행 화살표, 에너지 바(툴팁 FE).
- **기계**: `crusher`(20 FE/t, 100틱), `electric_furnace`(바닐라 `RecipeType.SMELTING` 레시피 사용, 20 FE/t, 100틱 고정).
- **분쇄 레시피(가루 x2)**: iron_ore, deepslate_iron_ore, copper_ore, deepslate_copper_ore, gold_ore, deepslate_gold_ore, tin_ore, deepslate_tin_ore(+raw_ 광석류도 동일). 석탄→coal_dust 없음. 가루 아이템: iron_dust, copper_dust, gold_dust, tin_dust. 가루→주괴는 바닐라 화로/용광로 레시피(JSON).
- **주석**: `tin_ore`, `deepslate_tin_ore`, `raw_tin`, `raw_tin_block` 생략(불필요), `tin_ingot`. 월드젠: configured/placed feature + `neoforge:add_features`(Y -16~112, 청크당 약 8회, 크기 8). 곡괭이 mineable + needs_stone_tool, 루트테이블(광석은 raw_tin, 행운 적용, 실크터치 시 자기 자신).
- **청동**: `bronze_ingot` 작업대 무형 레시피(구리 3 + 주석 1 → 청동 4). 
- **`machine_casing`**: 작업대 `B I B / I _ I / B I B` (B=bronze_ingot, I=iron_ingot) → 1개.
- **기계 레시피**: 분쇄기 `F F F / B C B / I R I`(F=flint, B=bronze_ingot, C=machine_casing, I=iron_ingot, R=redstone). 전기 용광로 `I I I / B C B / R F R`(F=minecraft:furnace, R=redstone).
- 공통: 광물 가공 체인 전체 검증(분쇄→가루→용광로→주괴).

### M1e – 합금·압축·회로 (확정)
- 레시피 타입 추가(M1d 헬퍼 재사용): `ironworks:alloying`, `ironworks:compressing`, `ironworks:circuit_assembly`.
- 기계(모두 `MachineBlockEntity` 파생, 공용 `MachineMenu` 재사용; 입력 슬롯 수 2/1/4):
  - `alloy_furnace` 합금로: 입력 2, 30 FE/t. 레시피: 청동(구리 주괴 3 + 주석 주괴 1 → 청동 4, 100틱), 강철(철 주괴 1 + 석탄 1 → 강철 1, 200틱; 숯 버전 별도), 플라스틱(`rubber` 2 + 석탄 1 → `plastic_sheet` 2, 120틱; 숯 버전 별도).
  - `compressor` 압축기: 입력 1, 30 FE/t, 100틱. 철/구리/강철 주괴 → `iron_plate`/`copper_plate`/`steel_plate`, 금 주괴 → `gold_foil` 2, `silicon` → `silicon_wafer`.
  - `circuit_assembler` 회로 조립기: 입력 4, 40 FE/t. `basic_circuit` = plastic_sheet 1 + iron_plate 1 + gold_foil 2 + redstone 1 (200틱). `advanced_circuit` = basic_circuit 1 + silicon_wafer 2 + gold_foil 2 + redstone 1 (300틱, 50 FE/t).
- 실리콘: `silicon_ore`, `deepslate_silicon_ore`(돌/심층암 대체, Y 0~64, 청크당 6회, 크기 6), 드롭 `silicon`(행운 적용, 실크터치 시 자기 자신), 분쇄기에서도 광석 → 실리콘 2(분쇄 레시피 추가).
- 아이템: steel_ingot, iron_plate, copper_plate, steel_plate, gold_foil, plastic_sheet, silicon, silicon_wafer, basic_circuit, advanced_circuit.
- 기계 제작: 합금로 `K K K / B C B / I F I`(K=minecraft:brick, B=bronze_ingot, C=machine_casing, I=iron_ingot, F=minecraft:furnace). 압축기 `I P I / B C B / I R I`(P=minecraft:piston, R=redstone). 회로 조립기 `G R G / S C S / I W I`(G=gold_ingot, S=steel_ingot, W=copper_cable).
- 4입력 기계의 GUI: 슬롯 겹침이 없도록 좌표를 계산해 배치하고(2x2), 출력 슬롯과 진행 화살표 위치를 보고할 것.

### M2 상세 – 컨베이어 (확정)
- **`conveyor`** (`ConveyorBlock` + `ConveyorBlockEntity`): `facing`(수평 4방향), 얇은 슬랩 모양(높이 4/16) VoxelShape. BE는 **아이템 1개 버퍼**와 `progress`(0~8틱)를 가진다.
  - 입력: ItemHandler capability(앞면 제외 모든 면에서 삽입 가능, 추출 불가, 버퍼가 비어 있을 때만 1개 수락). 뒤쪽 블록이 ItemHandler(상자·기계 출력 슬롯 등)를 노출하면 **끌어오기**: 버퍼가 비었을 때 8틱마다 1개 추출(`extractItem`). 위에 떨어진 `ItemEntity`도 흡수.
  - 이동: 서버 틱에서 `progress`를 올리고 8이 되면 앞 블록으로 전달. 앞이 컨베이어/분배기/필터면 그쪽 버퍼에 삽입, 그 외에는 `Capabilities.ItemHandler.BLOCK`(앞 블록, 반대 면)에 `insertItem`; 앞이 비어 있고 막혀 있지 않으면 아이템 엔티티로 떨어뜨리지 **않는다**(막히면 대기).
  - 동기화: 아이템 수락/전달 시에만 `sendBlockUpdated`(업데이트 패킷), 클라이언트는 `progress`를 보간. 렌더링은 `BlockEntityRenderer`로 아이템을 선 위에 보간해 그린다(등록: `EntityRenderersEvent.RegisterRenderers`).
  - 성능: 버퍼가 비고 뒤쪽 소스가 없으면 틱 비용 최소(8틱마다 한 번만 뒤쪽 조회).
  - NBT 저장: 버퍼 아이템, progress.
  - 레시피: `R R R / S I S`(R=rubber, S=steel_plate, I=iron_ingot) → 6개.
- **`splitter`** 분배기: 입력을 받으면 앞/좌/우 3방향에 라운드로빈으로 전달(받는 쪽이 거절하면 다음 방향 시도). 레시피(무형): 컨베이어 3 + iron_plate 1 → 1개.
- **`sorter`** 필터: 우클릭(아이템 들고)으로 필터 아이템 지정, 맨손 웅크리기 우클릭으로 해제, 액션바에 현재 필터 표시. 필터와 일치(아이템 종류 비교)하면 앞으로, 불일치하면 좌/우로 전달. 레시피: `S P S / P C P / S P S`가 아니라 단순히 무형 컨베이어 1 + hopper 1 + redstone 1 → sorter 1.
- 공통: 모든 컨베이어류는 곡괭이 mineable, 루트테이블(자기 자신), 크리에이티브 탭, lang(en_us/ko_kr), 임시 텍스처(gen_textures.py), blockstates(facing 4), models.

### M1 부속 – 광물·합금 (확정)
- 광물(월드젠 JSON): **주석**(Y 0 이상 지표 위주), **실리콘/규석**(모래 지형). 구리·철·석탄은 바닐라 사용. 은·희토류는 M4 후보.
- 합금: **청동** = 구리 3 + 주석 1, **강철** = 철 + 석탄. 스테인리스는 만들지 않는다(니켈도 제외).
- 기계 티어: 철(바닐라) → 청동 → 강철. 컨베이어·서버랙 프레임은 강철.
- `AlloyFurnace`(합금로): 입력 2칸 → 출력 1칸. 레시피는 JSON 레시피 타입 `ironworks:alloying`.
- 가공 체인: 광석 → 분쇄기(가루 x2) → 용광로(주괴) → 합금로/압축기(판).
- 드라이브 재료: 실리콘 웨이퍼 + 강철 케이스 + 구리선(회로).

### M1a – 고무나무 (확정)
- 독자 블록: `rubber_log`(`axis` + `sap` 불리언 상태), `rubber_planks`, `rubber_leaves`, `rubber_sapling`. 바닐라 원목은 쓰지 않는다.
- 수액: `sap=true`인 원목을 우클릭하면 `rubber_sap` 1개를 주고 `sap=false`로 바뀐다. `sap=false`인 원목은 랜덤 틱으로 확률 재생(기계 추출기는 M2 이후).
- 고무: `rubber_sap`을 바닐라 화로에 구우면 `rubber`(수작업 단계). 이후 가공기로 플라스틱 → 기초 회로로 이어진다.
- 월드젠: JSON(`configured_feature` → `placed_feature` → `neoforge:add_features` 바이옴 모디파이어, 숲 계열). 묘목은 `TreeGrower`로 같은 나무를 키운다.
- 태그: `minecraft:logs`, `logs_that_burn`, `planks`, `leaves`, `saplings`, `mineable/axe`, `mineable/hoe`(잎) 에 등록, 자체 태그 `ironworks:rubber_logs`.
- 재료 체인: 고무 → 플라스틱(고무 + 석탄) → 기초 회로(플라스틱 + 철 + 금 + 레드스톤) → 고급 회로(+실리콘).

### M2 – 컨베이어(공장 자동화)
- `ConveyorBlock`(FACING 프로퍼티, 위의 `ItemEntity`/플레이어 이동), 아이템 이동은 엔티티 push 대신 **BE 내부 1슬롯 버퍼**: 버퍼 아이템을 앞 칸 `Capabilities.ItemHandler.BLOCK`에 넘김, 없으면 앞 컨베이어로 인계(진행도 0~1 렌더링).
- 기계 입출력: 기계의 `IItemHandler`를 컨베이어가 삽입/추출. 분배기(Splitter: 좌/우/직진 순환), 필터(아이템 지정).
- 틱 비용 억제: 버퍼 비었고 이웃 없으면 틱 스킵 플래그.
- 완료 기준: 호퍼 없이 벨트 → 분쇄기 → 벨트 → 상자 자동 라인.

### M3 – 서버랙 / 스토리지 DB
- `DriveItem`(티어 1K/4K/16K, 데이터는 **DataComponent** `DriveData`: `Map<ItemKey,long count>` + 사용 용량, Codec/StreamCodec 직렬화).
- `ServerRackBlock(+BE)`: 드라이브 슬롯 8개(`ItemStackHandler`, 드라이브만 허용), 전력 소비(드라이브 수 비례).
- `NetworkCableBlock`: 랙/터미널/인터페이스를 연결, BFS로 네트워크 구성(블록 변경 시에만 재계산).
- `DbTerminalBlock`: GUI에서 **쿼리**: 이름 검색, 모드/태그 필터, 정렬(수량/이름), 꺼내기/넣기. 쿼리는 서버 측 `DbQuery`(인덱스 = 이름→키 TreeMap)에서 실행, 결과 페이지네이션 패킷으로 전송(전체 전송 금지).
- 데이터 무결성: 드라이브 제거 시 데이터는 아이템에 남음, 랙 전원 off 시 접근 불가.
- 완료 기준: 랙에 드라이브 삽입 → 케이블 → 터미널에서 넣기/검색/꺼내기, 월드 재접속 후 유지.

### M4 – 폴리시
- 레시피 체인, 한/영 언어, 툴팁, 사운드, 밸런스.

## 4. 구현 규약 (Haiku 준수)
1. 한 번에 한 마일스톤의 한 작업 단위만 구현하고, 끝나면 `./gradlew build` 결과를 보고한다. 실패 시 로그 전문을 첨부.
2. NeoForge API를 추측하지 말 것: 컴파일 오류가 나는 API는 임의 우회 대신 보고.
3. 서버/클라이언트 분리(`level.isClientSide`), 네트워크 패킷은 `CustomPacketPayload` 사용.
4. 커밋은 마일스톤 단위로, 브랜치 `claude/wizardly-noether-oklip5`.

## 5. 위험요소
- 현재 샌드박스는 NeoForge/Mojang Maven이 차단되어 **컴파일 검증 불가** → 네트워크 허용 필요.
- 스토리지 DB는 패킷/동기화가 가장 복잡 → 마지막에 구현.
