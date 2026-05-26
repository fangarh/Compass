# 2026-05-26 field GPS exchange handoff

## Короткий статус

Последний запушенный коммит перед этой заметкой:

- `a67aba5 Harden field GPS exchange`
- ветка: `main`
- remote: `origin/main`

Суть текущего состояния: мы ушли от неподвижных якорей к обмену координатами участников. Участник рисуется на карте только если от него есть пригодные GPS-координаты, полученные напрямую или через соседей. Если координаты отсутствуют или явно невозможны, телефон остается радио-соседом без пространственной точки.

## Устройства и роли

Полевые телефоны:

- Mi / `zhenya` / ADB `83efb856`
- OnePlus / `vasya` / ADB `e089985a`
- Samsung / `petya` / ADB `R3CT20C8A8N`

После последней установки все три телефона были на актуальном debug APK и запускались без `AndroidRuntime` crash в логах. На момент создания этой заметки `adb devices` на текущей машине уже не показывает подключенных устройств, поэтому в офисе первый шаг - подключить телефоны и проверить `adb devices`.

## Главный вывод по багу МИ

Дикая дистанция и улет координат были вызваны не нашей формулой расстояния и не сериализацией.

На МИ сам Android Location Provider выдавал невозможный GPS около:

```text
lat=0.570316
lon=0.170738
```

Это район Null Island. Поэтому при сравнении с Петербургом получались тысячи километров. Приложение раньше принимало эту координату как валидную и распространяло ее дальше по BLE / Wi-Fi Direct / UDP, из-за чего другие телефоны могли рисовать МИ как точку на огромной дистанции.

После фикса такие координаты считаются `GPS_OUTLIER`: МИ не должен отдавать их наружу и не должен появляться как координатная точка. Он остается `radio-only`, пока Android не даст нормальный GPS.

## Что было изменено

Ключевые новые/измененные классы:

- `app/src/main/java/net/afterday/compas/iff/IffGpsSanity.java` - фильтр невозможных координат, включая окрестность Null Island.
- `IffForegroundRadioService.java` - карантин локального GPS: плохая координата очищает `latestLocation`, удаляет локальную participant-точку, переводит статус в `GPS OUTLIER radio-only`, не отправляет GPS payload.
- `IffBleFieldRadio.java` - не рекламирует плохой GPS.
- `IffWifiDirectDiscoveryTransport.java` - не рекламирует плохой GPS.
- `IffUdpWitnessTransport.java` - не передает плохой GPS в witness report.
- `IffGpsSnapshot.java` - умеет создавать outlier snapshot и не принимает невозможные пары координат.
- `IffParticipantMapModel.java` - не рисует участников с невозможными координатами.
- `IffParticipantStore.java` - добавлен `remove(...)`, чтобы удалять локальную испорченную self-точку.

Связанные части текущей концепции:

- `IffParticipantState.java`, `IffParticipantStore.java`, `IffParticipantMapModel.java` - участники как результат обмена координатами.
- `IffCoordinateMessage.java` - координатное сообщение.
- `IffTacticalMapView.java` и `IffActivity.java` - экран карты/радара и отображение имен/дистанций/статусов.

## Что проверено

Перед коммитом `a67aba5` проходили:

```powershell
.\scripts\test-iff-coordinate-core.ps1
.\scripts\test-iff-ble-payload.ps1
.\scripts\test-iff-wifi-direct-payload.ps1
gradle :app:assembleDebug
```

Результат:

- coordinate core test passed;
- BLE payload test passed;
- Wi-Fi Direct payload test passed;
- `gradle :app:assembleDebug` -> `BUILD SUCCESSFUL`.

APK ставился и запускался на:

- Mi `83efb856`
- OnePlus `e089985a`
- Samsung `R3CT20C8A8N`

После фикса в свежих логах:

- МИ видел Samsung по радио, но сам шел как `gps=false` / `GPS_UNAVAILABLE` или outlier quarantine;
- Samsung и OnePlus принимали МИ как `gps=false`;
- OnePlus видел Samsung как координатную точку, МИ - как радио-соседа;
- дистанции порядка 7100 км от МИ больше не появлялись.

## Как продолжить в офисе

1. Обновить код:

```powershell
git pull
```

2. Подключить телефоны и проверить:

```powershell
adb devices
```

Ожидаемые ID:

```text
83efb856      Mi / zhenya
e089985a      OnePlus / vasya
R3CT20C8A8N   Samsung / petya
```

3. Собрать и поставить APK, если телефоны не на свежей версии:

```powershell
gradle :app:assembleDebug
adb -s 83efb856 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s e089985a install -r app\build\outputs\apk\debug\app-debug.apk
adb -s R3CT20C8A8N install -r app\build\outputs\apk\debug\app-debug.apk
```

4. Перед прогоном очистить старые логи на телефонах и запустить приложение. Если используется текущая привычная процедура подготовки телефонов, применять ее без изменения концепции.

5. В тесте смотреть отдельно:

- МИ не должен загрязнять сеть координатами `0.x / 0.x`;
- если GPS на МИ плохой, это нормальный результат: он должен быть радио-соседом без точки;
- Samsung/OnePlus не должны рисовать МИ на тысячах километров;
- координатные точки должны появляться только у телефонов с валидным GPS;
- обновление рабочих данных должно быть не реже одного раза в 2 секунды.

## На что обратить внимание дальше

- Если МИ снова "не виден и не видит", сначала проверить разрешения/сервисы/сканирование радио, но не возвращать плохой GPS в сеть.
- Если карта пустая, различать две ситуации:
  - радио-соседи есть, но GPS у них отсутствует или отфильтрован - это правильное поведение;
  - нет ни радио, ни GPS - это проблема discovery/permissions/service.
- Если OnePlus или Samsung показывают кривую дистанцию 25-70 м при слабом GPS, смотреть `gpsAccuracyM`, `gpsAgeMs`, `gpsStatus`, `participantMapStatus`.
- Для реальной дистанции ориентироваться на голые GPS-координаты только при адекватной точности. При `accuracy > 30m` дистанция должна считаться слабым ориентиром, а не истиной.

## Важное правило

Не лечить Null Island математикой расстояния. Невозможная координата должна быть отброшена до попадания в payload, participant store и карту.

