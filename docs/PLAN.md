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
