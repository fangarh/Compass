# 2026-05-26 radar scale and display-name handoff

## Короткий статус

На Samsung и OnePlus установлен debug APK с двумя пользовательскими изменениями:

- под радаром добавлены кнопки `-` / `+` и цифровой индикатор `SCALE ...` для ручного изменения масштаба;
- изменение имени телефона теперь передается другим участникам по рабочим радиоканалам.

Финальная живая проверка выполнялась на двух телефонах:

- Samsung `R3CT20C8A8N`: `petya`, `PHONE_C_MOVING_TARGET`, display name `DDS`, radio on;
- OnePlus `e089985a`: `vasya`, `PHONE_A_WITNESS`, radio on.

Mi в финальной проверке не участвовал. OnePlus после тестов был отправлен заряжаться.

## Логи 08:20-08:21

По запросу были сняты Samsung и OnePlus за окно `08:20-08:21`:

- `artifacts/field-logs-20260526-radar-0820-0821/R3CT20C8A8N/field-radio-20260526-082012-0820-0821.log`
- `artifacts/field-logs-20260526-radar-0820-0821/e089985a/field-radio-20260526-003241-0820-0821.log`
- сводный анализ: `artifacts/field-analysis-20260526-radar-0820-0821/`

Вывод по кривому направлению радара: GPS в этом окне был пригодным, но BLE вокруг Samsung был редким и асимметричным. В логах есть выбросы `rssi=127` и малое число семплов по соседям, поэтому офисный proximity/direction в тот момент не мог быть стабильным только по радио.

## Масштаб радара

Добавлено:

- `app/src/main/java/net/afterday/compas/iff/IffMapScale.java`
- `scripts/test-data/iff-map-scale/IffMapScaleTest.java`
- `scripts/test-iff-map-scale.ps1`

Изменено:

- `IffActivity.java` - под картой добавлены `-`, `SCALE ...`, `+`; выбранный масштаб хранится в prefs `map_scale_range_meters`;
- `IffTacticalMapView.java` - радиус точки теперь считается из реальной дистанции и выбранного диапазона карты, а не как прежняя фиксированная эвристика.

Доступные масштабы: `25m`, `50m`, `75m`, `150m`, `300m`.

Проверено на телефонах: кнопка `+` на OnePlus меняла `SCALE 75m` на `SCALE 50m`, затем масштаб был возвращен на `75m`.

## Передача имени участника

Проблема: локальное имя можно было поменять на телефоне, но другие участники продолжали видеть fallback по `playerId`.

Исправлено несколькими слоями, потому что в реальном офисном тесте стабильнее работал BLE, а Wi-Fi Direct TXT не всегда приходил в коротком окне:

- `IffWifiDirectPayload.java` теперь несет `dn` / `displayName`;
- `IffWifiDirectDiscoveryTransport.java` публикует текущее имя в TXT payload и принимает его от соседей;
- `IffParticipantDisplayNames.java` хранит известные имена участников и не дает BLE fallback перетирать уже выученное имя;
- `IffForegroundRadioService.java` передает локальное имя в радио-слой и запоминает имена из входящих participant-состояний;
- `IffBlePayload.java` получил контракт v3 с short callsign;
- `IffBleFieldRadio.java` рекламирует локальное имя в BLE v3 и запоминает имя из BLE-пакетов соседей.

Добавлено:

- `app/src/main/java/net/afterday/compas/iff/IffParticipantDisplayNames.java`
- `scripts/test-data/iff-participant-display-names/IffParticipantDisplayNamesTest.java`
- `scripts/test-iff-participant-display-names.ps1`

Обновлены тесты:

- `scripts/test-data/iff-ble-payload/IffBlePayloadTest.java`
- `scripts/test-data/iff-wifi-direct-payload/IffWifiDirectPayloadTest.java`

## Живая проверка имени

На Samsung было вручную выставлено имя `DDS`. После сборки и установки APK на Samsung и OnePlus OnePlus увидел Samsung как:

```text
playerId=petya displayName=DDS sourcePlayerId=ble-petya
```

Это подтверждает, что переименование Samsung реально дошло до другого телефона по рабочему BLE path.

Ограничение BLE v3: short callsign несет до 5 UTF-8 bytes. Поэтому русское `Вася` по BLE может отображаться как `Ва`. Полное имя остается доступным через Wi-Fi Direct / координатные participant-сообщения, когда этот канал реально доходит.

## Проверки

Успешно прошли:

```powershell
.\scripts\test-iff-ble-payload.ps1
.\scripts\test-iff-wifi-direct-payload.ps1
.\scripts\test-iff-participant-display-names.ps1
.\scripts\test-iff-map-scale.ps1
```

Сборка:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
& 'C:\Users\Admin\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat' :app:assembleDebug --no-daemon
```

Результат: `BUILD SUCCESSFUL`.

Также выполнен `git diff --check`; критических whitespace-ошибок нет, остались только ожидаемые предупреждения о CRLF в новых/измененных файлах.

`scripts\test-iff-coordinate-core.ps1` в этой сессии уперся в cleanup-lock на `.class`-файле, а не в поведенческое падение теста. Это стоит отдельно почистить при следующем подходе, если снова потребуется полный локальный прогон coordinate core.

## Что делать дальше

- Если радар снова показывает странное направление, сначала смотреть `gpsAccuracyM`, `distanceM`, `bearingDeg`, выбранный `SCALE`, а затем уже BLE/RSSI.
- Для малых дистанций использовать `SCALE 25m` или `50m`; для разнесенных телефонов `150m` или `300m`.
- Для имени считать BLE v3 быстрым коротким каналом, а Wi-Fi Direct / coordinate payload - каналом полного имени.
- Перед коммитом не включать случайный `test.png`, если он не относится к задаче.
