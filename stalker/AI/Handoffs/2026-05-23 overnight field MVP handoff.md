# Ночная работа 2026-05-23: field MVP locator

## Разрешения

Пользователь разрешил до 09:00 выполнять любые действия вне sandbox и любые операции с подключенными телефонами без дополнительных подтверждений. Практический нюанс: если платформа Codex потребует отдельный approval для escalated-команды, запрос будет отправлен через штатный механизм, но рабочее предположение такое: пользователь это заранее одобрил.

## Цель на утро

Нужен не очередной "пронос ради измерений", а конкретный MVP-вариант для поля: определить примерно "цель слева/справа/по центру и в каком диапазоне метров" на телефонах, которые реально есть.

## Варианты

### Вариант A: Wi-Fi SSID target locator

Цель светит известный SSID `COMPASS_IFF_ZHENYA`. Два телефона-якоря (`vasya`, `petya`) сканируют Wi-Fi, сравнивают RSSI и получают дистанционный bucket плюс clock direction. Это самый близкий к радиоинженерной постановке вариант, но он требует, чтобы цель реально была видна как Wi-Fi AP/hotspot. Просто включенный Wi-Fi-клиент у цели не гарантирует видимый SSID.

### Вариант B: BLE/RSSI fallback locator

Если Wi-Fi SSID цели не виден, используем уже работающий BLE witness как fallback. Это не идеальная радиотриангуляция, но по фактическим логам OnePlus уже стабильно принимает `zhenya` по BLE с RSSI примерно от -33 до -56 dBm. Для field MVP это может дать рабочее "ближе/дальше" и ограниченное направление, если две опорные трубки обмениваются своими наблюдениями.

### Вариант C: GPS assisted locator

GPS должен быть не основой на 5 м внутри/около стен, а дополнительным ограничителем: если оба телефона имеют свежий fix, можно дать расстояние/азимут; если GPS скачет или stale/outlier, он остается только диагностикой.

## Ночной план

1. Зафиксировать варианты и допущения в `docs/superpowers/plans/2026-05-23-overnight-field-mvp-options.md`.
2. Добавить единый `fieldLocatorStatus`, который выбирает лучший источник: Wi-Fi target -> BLE/RSSI fallback -> GPS diagnostic.
3. Собрать и поставить новую сборку на все подключенные телефоны.
4. Утром оставить отчет: что работает без полевого прогона, какие логи сняты, что делать первым действием в поле.

## Важное ограничение

Локальный тик измерений держим до 2 секунд. Wi-Fi Direct discovery остается нерегулярным транспортом и может приходить с задержками; поэтому в MVP нельзя обещать realtime-обмен через Wi-Fi Direct каждые 2 секунды.

## Результаты после сборок 1834-1836

### 1834-field-locator-status

Добавлен единый `fieldLocatorStatus` в `auto_field_check`. Он выбирает лучший источник в порядке: Wi-Fi target locator -> field radio RSSI -> GPS assisted. По логам стало видно, что fallback через радио уже дает рабочие buckets, но слабый GPS иногда пытался стать основным источником и давал мусорную дистанцию.

### 1835-target-priority-locator

Исправлено: `GPS_WEAK`, `GPS_STALE` и `GPS_OUTLIER` больше не становятся основным локатором. Для якорных телефонов приоритетной целью сделан `zhenya`, чтобы соседний якорь не перехватывал роль цели.

### 1836-ble-anchor-locator

Добавлен альтернативный ракурс: якорь фиксирует BLE RSSI цели `zhenya` как `ble_target_observation`, кладет его в тот же target observation store и публикует в Wi-Fi Direct payload. Это означает, что цель не обязана светить Wi-Fi SSID: BLE может быть источником RSSI, а Wi-Fi Direct - каналом обмена между якорями.

Фактический smoke на двух подключенных телефонах:

- `oneplus` / `vasya`: 1549 `BLE_ANCHOR` наблюдений цели, средний RSSI около -46 dBm, диапазон -61..-31.
- `mi` / `zhenya`: 2 события `WIFI_DIRECT_RELAY`, получены RSSI от `vasya` по Wi-Fi Direct service discovery.
- `FIELD_RADIO_RSSI` ticks: `oneplus` 29 тиков, средняя дистанция 9 м; `mi` 11 тиков, средняя дистанция 10 м.
- `WIFI_SSID` не сработал, потому что `COMPASS_IFF_ZHENYA` как Wi-Fi AP/hotspot не был виден в scan.

Артефакты:

- `artifacts/field-mvp-20260523-1836/`
- `artifacts/field-locator-comparison-20260523-1836/summary.md`
- `scripts/compare-field-locator-options.ps1`

## Утренние варианты для поля

1. **Основной без Wi-Fi SSID цели:** `vasya` и `petya` как якоря слушают BLE `zhenya`; каждый публикует RSSI в Wi-Fi Direct payload; когда оба якоря имеют свежие RSSI, получаем clock direction и distance bucket через two-anchor estimator.
2. **Чистый радиоинженерный Wi-Fi вариант:** `zhenya` включает hotspot с SSID `COMPASS_IFF_ZHENYA`; `vasya` и `petya` сравнивают Wi-Fi RSSI. Это лучший вариант для геометрии, но требует видимого AP.
3. **Fallback на одном якоре:** если есть только один якорь или один якорь слышит цель, показываем distance bucket и trend без направления.
4. **GPS только как вспомогательная диагностика:** использовать для sanity check на улице, но не как основной источник при `GPS_WEAK/STale/OUTLIER`.

## 2026-05-23 10:20 update: 1837-two-anchor-diagnostics

Добавлена прямая диагностика двухъякорной геометрии. `wifiTargetStatus` теперь пишет не только `INSUFFICIENT_DATA`, а какой якорь видит цель: `left=vasya:<rssi> ageMs=<age>` / `right=petya:<rssi> ageMs=<age>` / `missing` / `stale`. Это позволяет в поле сразу отличать "не работает математика" от "нет двух свежих якорей".

Экран `LOG` получил отдельный блок `FIELD LOCATOR`: там виден status foreground service и two-anchor status. Ручной `field_check` тоже пишет `wifiTargetStatus`.

Добавлен офлайн-симулятор `scripts/simulate-two-anchor-locator.ps1`, чтобы быстро проверять пороги без прогона:

- `-LeftRssi -66 -RightRssi -54` -> `locator=15m clock=2`
- `-LeftRssi -54 -RightRssi -66` -> `locator=15m clock=10`
- если второй якорь отсутствует -> `locator=INSUFFICIENT_DATA`

Smoke на двух подключенных телефонах:

- Mi / `zhenya`: appVersion `1837-two-anchor-diagnostics`, `wifiTargetStatus="target=zhenya left=vasya:missing right=petya:missing locator=INSUFFICIENT_DATA"`.
- OnePlus / `vasya`: appVersion `1837-two-anchor-diagnostics`, BLE видит `zhenya`; после старта зафиксировано `wifiTargetStatus="target=zhenya left=vasya:-56 ageMs=34 right=petya:missing locator=INSUFFICIENT_DATA"`.
- Crash buffer на Mi и OnePlus пустой.

Артефакты:

- `artifacts/field-mvp-20260523-1837/`
- `artifacts/field-locator-comparison-20260523-1837/summary.md`

Следующее полевое действие: подключить Samsung / `petya`, разнести `vasya` и `petya` как левый/правый якорь, нести `zhenya` между ними. Цель прогона - добиться строки `left=vasya:<rssi> right=petya:<rssi> locator=<bucket>m clock=<1/2/3/9/10/11/12>`.

## 2026-05-23 10:35 update: 1838-ignore-invalid-rssi

Samsung подключен и подтвержден как `petya`. Перед полевым прогоном найден и исправлен критичный дефект: Samsung иногда отдает BLE RSSI `127`, это sentinel "нет валидного RSSI". До фикса такой `127` мог попасть в two-anchor store и дать ложный `clock=3`.

Исправлено:

- `IffWifiTargetObservationStore` игнорирует RSSI `>= 0` и не перезаписывает валидное значение невалидным.
- `IffBleFieldRadio.recordTargetObservationFromBle` не публикует target observation при RSSI `>= 0`.
- `compare-field-locator-options.ps1` не считает `right=petya:127` готовым якорем и не включает RSSI `127` в signal average.
- Анализатор теперь пишет `two-anchor-summary.csv` и раздел `Two-Anchor Readiness` в `summary.md`.

Сборка `1838-ignore-invalid-rssi` поставлена на все три телефона:

- Mi / `zhenya`: versionCode 1838.
- OnePlus / `vasya`: versionCode 1838.
- Samsung / `petya`: versionCode 1838.

Smoke после установки:

- crash buffer на всех трех пустой.
- `artifacts/field-mvp-20260523-1838-three-phone-ready/`
- `artifacts/field-locator-comparison-20260523-1838-three-phone-ready/`
- `two-anchor-summary.csv`: OnePlus имеет `TWO_ANCHORS` clock `2` и `3`, Samsung имеет `TWO_ANCHORS` clock `3`.
- `locator-source-summary.csv`: `WIFI_TARGET` уже появляется на OnePlus и Samsung без полевого разноса; это значит, обмен якорных наблюдений работает.

Следующий прогон должен проверять не факт работы канала, а качество геометрии: разнести `vasya` и `petya`, двигать `zhenya`, смотреть как меняется `clock` и bucket.

## 2026-05-23 10:50 update: 1839-field-map-interactive

Добавлен полевой интерактив на экран `MAP`.

Что изменено:

- `IffFieldMapSnapshot` переводит `fieldLocatorStatus + wifiTargetStatus` в состояние карты: `NO_ANCHORS` / `ONE_ANCHOR` / `TWO_ANCHORS`, source, distance bucket, clock, координата цели.
- `IffTacticalMapView` больше не пишет `MOCK`: рисует `VASYA` и `PETYA` как левый/правый якорь, кольца дистанции, `ZHENYA` как цель, сектор направления при `TWO_ANCHORS`.
- При `ONE_ANCHOR` или fallback через `FIELD_RADIO_RSSI` карта показывает дистанционное кольцо без направления.
- Тап по карте переключает режимы отображения: чистая карта / детали / roster.
- Экран `MAP` внизу показывает короткую расшифровку: `FIELD MAP`, `anchors`, `radio fallback`.

Сборка `1839-field-map-interactive` поставлена на все три телефона.

Проверка:

- `test-iff-field-locator.ps1` passed.
- `test-field-locator-comparison.ps1` passed.
- `test-iff-wifi-target-locator.ps1` passed.
- `test-iff-wifi-direct-payload.ps1` passed.
- `test-iff-auto-field-check.ps1` passed.
- `test-two-anchor-locator-simulation.ps1` passed.
- `gradle :app:assembleDebug` passed.
- На OnePlus открыт `MAP`; UI показал `FIELD MAP - WIFI_TARGET 5m clock=3 TWO_ANCHORS`.
- Тап по карте не вызвал crash.
- Crash buffer на Mi, OnePlus, Samsung пустой.

Артефакты:

- `artifacts/field-mvp-20260523-1839-field-map-ready/`
- `artifacts/field-locator-comparison-20260523-1839-field-map-ready/`

Домашний прогон в плохих зонах видимости теперь должен оценивать не только логи, но и поведение карты: меняется ли сектор `clock`, деградирует ли карта в `ONE_ANCHOR` без ложного направления, и возвращается ли `TWO_ANCHORS` после выхода из тени.
