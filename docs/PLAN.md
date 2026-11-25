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

### M3a 상세 – 드라이브·서버랙·네트워크 (확정)
**목표**: 서버랙에 드라이브(저장장치)를 꽂아 "DB"로 쓴다. 이 단계는 저장 엔진과 네트워크(넣기/꺼내기 API, 포트, 케이블)까지이고, 검색·정렬 GUI(터미널)는 M3b.
- **데이터 컴포넌트** `ironworks:drive_data` (`DriveData` record): 항목 리스트 `List<Entry(ItemStack template(count 1), long count)>`. Codec + StreamCodec, persistent + network 동기화. 컴포넌트는 `DeferredRegister.DataComponents` 로 등록. 드라이브 아이템 자체에 데이터가 남으므로 드라이브를 뽑아도 내용 유지.
- **`DriveItem`**(스택 1개): 티어별 `drive_1k`(총 1,024개 · 종류 16), `drive_4k`(4,096 · 32), `drive_16k`(16,384 · 63). 툴팁: `사용 123 / 1,024 · 종류 3 / 16` + 상위 항목 몇 개. 용량 초과·종류 초과 시 삽입은 가능한 만큼만 수락. 컴포넌트 비교 기준: 같은 아이템 + 같은 컴포넌트(`ItemStack.isSameItemSameComponents`)를 같은 종류로 취급.
- **저장 인터페이스** `storage/IStorageProvider` (서버랙 BE 구현): `long insert(ItemStack template, long count, boolean simulate)`(삽입된 양 반환), `long extract(ItemStack template, long count, boolean simulate)`(추출된 양 반환), `Map<ItemKey, Long> snapshot()`, `boolean isOnline()`. 삽입 우선순위: 같은 종류를 이미 가진 드라이브 → 빈 종류 칸이 있는 드라이브. `ItemKey`는 아이템+컴포넌트 기반 해시/동등성.
- **`server_rack`** (`ServerRackBlock`+BE, facing + `online` 블록상태): 드라이브 슬롯 8개(`ItemStackHandler`, DriveItem 만 허용). 에너지 버퍼 5,000 FE(수신 128/틱, 외부 추출 불가), 소비 = 틱당 2 FE × 설치된 드라이브 수 + 1. 전력이 있으면 online, 없으면 offline(삽입·추출·스냅샷 모두 거부). GUI: 2×4 드라이브 슬롯 + 플레이어 인벤토리 + 에너지 바(공용 `MachineScreen` 스타일 재사용 또는 전용). 블록 제거 시 드라이브 드롭(데이터 유지).
- **`network_cable`** (`NetworkCableBlock`, BE 없음): 6면 연결(구리 케이블과 같은 방식의 상태 프로퍼티·멀티파트 모델), 연결 대상은 다른 네트워크 케이블, `server_rack`, `storage_port`, (M3b) 터미널. 에너지 전송 없음(데이터 전용 케이블).
- **`StorageNetwork`** 유틸: 시작 위치에서 케이블/노드를 BFS(최대 256 블록, 로드된 청크만)해 `IStorageProvider` 목록을 수집, 합산 `insert/extract/snapshot` 제공. 결과는 20틱 캐시(블록 변경 시 무효화 필요 없음, 단순 시간 캐시). 서버 측에서만 사용.
- **`storage_port`** (`StoragePortBlock`+BE): 네트워크에 붙는 입구. `Capabilities.ItemHandler.BLOCK` 노출: 삽입 시 `StorageNetwork.insert`(전량 수락 못하면 남은 스택 반환), 추출 불가. 컨베이어/호퍼 → 포트 → 네트워크로 자동 입고. 인접한 케이블/랙을 통해 네트워크를 찾음(포트 자신도 네트워크 노드).
- **레시피**(작업대): `drive_1k` = `S P S / P B P / S R S` (S=steel_plate, P=plastic_sheet, B=basic_circuit, R=redstone), `drive_4k` = `S P S / W A W / S R S` (W=silicon_wafer, A=advanced_circuit), `drive_16k` = `A W A / S D S / A W A` (D=minecraft:diamond), `server_rack` = `S B S / C K C / S R S`(C=machine_casing, K=ironworks:copper_cable, B=basic_circuit), `network_cable` = 무형 아님: `P P P / G G G / P P P` → 6개(P=plastic_sheet, G=gold_foil), `storage_port` = `S B S / H C H / S R S`(H=minecraft:hopper).
- **디버그 명령** `/ironworks storage <x y z>`(OP 전용): 해당 위치에서 BFS 로 찾은 네트워크의 online 여부, 랙 수, 스냅샷(종류별 수량)을 채팅으로 출력. 검증에 사용.
- 공통: lang(en_us/ko_kr), 임시 텍스처, blockstates/models, 루트테이블, mineable/pickaxe 태그, 크리에이티브 탭(드라이브 3 포함).

### M3b 상세 – DB 터미널 (확정, 2단계로 분할)
**M3b-1 쿼리 엔진 (서버 로직, 명령어로 검증 가능)**
- `storage/query/QueryRow`(record: `String namespace, String path, Set<String> tags, long count`) — Minecraft 클래스에 의존하지 않는 순수 데이터. 서비스가 네트워크 스냅샷(`Map<ItemKey,Long>`)을 `QueryRow` + 원본 템플릿으로 변환할 때 `ItemStack.getTags()`로 태그 문자열(`minecraft:logs` 형태)을 채운다.
- `storage/query/DbQuery`: 문자열 쿼리를 파싱해 `Predicate<QueryRow>` 로 만든다. 공백으로 구분된 토큰은 모두 AND. 토큰 종류: 일반 단어(경로/네임스페이스에 대한 부분 일치, 대소문자 무시, `_`와 공백 동일 취급), `@모드ID`(네임스페이스 일치), `#태그`(`#minecraft:logs`, 네임스페이스 생략 시 `minecraft:`와 `c:` 둘 다 허용), `>N` `<N` `>=N` `<=N` `=N`(수량 비교, `1k`/`2.5m` 같은 접미사 허용), 토큰 앞 `-` 는 부정(`-@minecraft`). 잘못된 토큰은 무시하지 않고 `DbQuery.ParseResult`의 `errors` 목록에 담아 반환(서비스는 오류가 있어도 나머지 조건으로 실행).
- `storage/query/SortMode`(`NAME`, `COUNT`, `ID`) + `descending` 불리언. 정렬은 안정 정렬, 동점은 레지스트리 ID 로 결정.
- `storage/query/QueryService.run(StorageNetwork, QueryRequest(query, sort, descending, page, pageSize))` → `QueryPage(List<Entry(ItemStack template, long count)>, int totalMatches, int page, int pageCount, boolean online, long usedItems, int rackCount)`. 페이지는 0부터, 범위를 벗어나면 마지막 페이지로 보정. 네트워크가 오프라인이면 빈 결과 + `online=false`.
- `QueryService.extract(network, ItemStack template, long amount)` / `insert(...)` 는 `StorageNetwork` 의 기존 API 를 감싸 결과 수량을 반환(터미널 패킷 처리와 명령어가 같은 메서드를 사용).
- JUnit 5 도입: `build.gradle` 에 `testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'`, `testRuntimeOnly 'org.junit.platform:junit-platform-launcher:1.10.2'`, `tasks.withType(Test){ useJUnitPlatform() }`. `DbQuery` 파서/필터를 `QueryRow` 만으로 테스트(부분 일치, `@`, `#`, 수량 비교와 접미사, 부정, AND 결합, 오류 토큰, 빈 쿼리 = 전체).
- 디버그 명령(OP): `/ironworks query <x y z> <쿼리…>`(마지막에 선택 인자 없이, 정렬/페이지는 `sort=count desc page=1` 같은 키=값 토큰을 쿼리 문자열 앞에서 분리), `/ironworks extract <x y z> <아이템> <수량>`(추출한 아이템을 해당 좌표 위에 드롭).

**M3b-2 터미널 블록 + 화면 + 패킷**
- `db_terminal` 블록(facing, 네트워크 케이블/랙/포트와 연결), 우클릭 시 `DbTerminalMenu`(플레이어 인벤토리 슬롯만, 결과는 가상). 제작: `S A S / C G C / S R S`(S=steel_plate, A=advanced_circuit, C=machine_casing, G=minecraft:glass, R=redstone).
- 패킷(`CustomPacketPayload` + `RegisterPayloadHandlersEvent`): C2S `QueryRequest`(쿼리, 정렬, 내림차순, 페이지), S2C `QueryResult`(`QueryPage` 내용), C2S `TakeRequest`(템플릿 ItemStack, 모드: 한 스택/절반/1개/쉬프트=인벤토리로), C2S `InsertRequest`(커서 전체/1개). 서버는 메뉴가 열려 있고 거리 ≤ 8 인지 검증, 수량 클램프, 템플릿은 `ItemKey` 로 재확인.
- 화면: 검색창(`EditBox`, 입력 후 0.3초 디바운스로 쿼리 전송), 정렬 버튼(이름/수량/ID, 오름·내림 토글), 9×5 결과 격자(아이템 렌더 + 수량 약어 1.2K/3.4M), 페이지 이전/다음, 상태줄(Online/Offline · 총 항목 수 · 랙 수), 쿼리 오류 표시. 결과 칸 클릭: 좌클릭=한 스택, 우클릭=절반, 쉬프트 좌클릭=인벤토리로. 커서에 아이템이 있을 때 결과 영역 클릭=넣기. 플레이어 인벤토리 쉬프트 클릭=넣기(`quickMoveStack`).
- 오프라인/빈 네트워크에서는 안내 문구만 표시.

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
