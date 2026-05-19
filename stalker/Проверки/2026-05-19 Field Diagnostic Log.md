# 2026-05-19 Field Diagnostic Log

## Цель

Добавить полевой диагностический лог на телефоне для Wi-Fi/radio тестов.
Лог должен сохраняться в файл на устройстве и забираться после теста через ADB.

## Устройство

- `R3CT20C8A8N`
- Samsung SM-S908B / S22 Ultra
- `e089985a`
- NE2215

Эмулятор не используется для выводов о реальной Wi-Fi радиокартине.

## Файл Лога

```text
/sdcard/Android/data/net.afterday.compas/files/diagnostics/field-radio-YYYYMMDD-HHMMSS.log
```

## Команда Скачивания

```powershell
New-Item -ItemType Directory -Force artifacts\field-logs
adb -s R3CT20C8A8N pull /sdcard/Android/data/net.afterday.compas/files/diagnostics artifacts\field-logs
```

## Анализ Логов

Команда:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\analyze-field-logs.ps1
```

Выходные файлы:

```text
artifacts/field-analysis/summary.md
artifacts/field-analysis/scan-summary.csv
artifacts/field-analysis/bucket-summary.csv
artifacts/field-analysis/event-summary.csv
artifacts/field-analysis/window-device-summary.csv
artifacts/field-analysis/device-comparison.csv
artifacts/field-analysis/device-context.csv
artifacts/field-analysis/movement-deltas.csv
```

`device-context.csv` заполняется логами Phase 2, где есть строка
`FIELD_DIAG event=device_context`. Старые Phase 1 логи такого header-а не имеют.
`movement-deltas.csv` показывает BSSID, у которых RSSI заметно изменился между
окнами теста.

Выходные файлы offline zone evaluator:

```text
artifacts/field-analysis/zone-fingerprints.csv
artifacts/field-analysis/zone-evaluation.csv
artifacts/field-analysis/zone-predictions.csv
```

`zone-fingerprints.csv` хранит Wi-Fi fingerprint по device/window/BSSID.
`zone-predictions.csv` сравнивает 30-секундные buckets с fingerprint-ами этого
же устройства и выбирает наиболее похожую зону.

Именованные окна можно передавать прямо в analyzer:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\analyze-field-logs.ps1 `
  -InputRoot artifacts\field-logs\run-20260519-1045 `
  -OutputDir artifacts\field-analysis-run-20260519-1045-named `
  -Windows "cabinet=10:45:10..10:47:10;corridor=10:47:10..10:49:10;cabinet_return=10:49:10..10:51:10;near_30cm=10:51:10.."
```

## Что Проверять В Логе

- `FIELD_DIAG event=logger_start`
- `WIFI_DIAG event=sensor_start`
- `WIFI_DIAG event=request`
- `WIFI_DIAG event=results`
- `updated=true` или `updated=false`
- `WIFI_DIAG event=scan_entry`
- SSID/BSSID/RSSI/frequency/timestamp/capabilities для видимых Wi-Fi сетей.

## Статус

Выполнено на физическом телефоне 2026-05-19.

## Результат

- Debug APK собран.
- APK установлен на `R3CT20C8A8N`.
- Приложение запущено.
- Файл создан на телефоне:

```text
/sdcard/Android/data/net.afterday.compas/files/diagnostics/field-radio-20260519-092839.log
```

- Файл скачан локально:

```text
artifacts/field-logs/diagnostics/field-radio-20260519-092839.log
```

- В логе подтверждены:
  - `FIELD_DIAG event=logger_start`
  - `WIFI_DIAG event=sensor_start`
  - `WIFI_DIAG event=request accepted=true`
  - `WIFI_DIAG event=results source=receiver updated=true`
  - `WIFI_DIAG event=scan_results source=cached updated=false`
  - `WIFI_DIAG event=scan_entry`

## Учет Разницы Телефонов

RSSI нельзя напрямую сравнивать как абсолютную дистанцию между разными
телефонами. У каждого телефона свой Wi-Fi чип, антенна, прошивка, политика
энергосбережения и чувствительность к положению корпуса. Один и тот же BSSID в
одной точке может отличаться на несколько dB между `R3CT20C8A8N` и `e089985a`.

Заряд батареи обычно не меняет RSSI напрямую, но может менять поведение
сканирования: частоту свежих результатов, throttling, фоновые ограничения и
агрессивность энергосбережения.

Рабочие правила анализа:

- Не переводить один RSSI напрямую в метры.
- Считать зоны: сильный, средний, слабый, нестабильный край, потеря.
- Сначала смотреть динамику внутри одного телефона.
- Для каждого телефона строить отдельный baseline.
- При сравнении двух телефонов группировать данные по device serial/model и BSSID.

В следующий инкремент полезно добавить в header лога модель устройства, SDK,
заряд батареи, зарядку, power-save mode, Wi-Fi state и location state.

## Маркеры Двухтелефонного Теста

Начальная точка:

```text
R3CT20C8A8N и e089985a лежат рядом, расстояние примерно 30 см.
```

Точка отхода:

```text
10:00 по времени этого ПК - второй телефон унесен на другой конец комнаты,
примерно 5 метров.
10:02 +/- 5 секунд по времени этого ПК - второй телефон унесен за дверь в коридор.
Относительный маркер - примерно за 15-20 секунд до сообщения пользователя
второй телефон положен в другой угол комнаты.
Финальный маркер - второй телефон возвращен и снова подключен по USB.
```

## Первый Анализ

Analyzer обработал 3 файла и 21416 `scan_entry`.

Свежие события `source=receiver updated=true`:

- `R3CT20C8A8N`: 838
- `e089985a`: 266

По окнам теста видно, что после выноса второго телефона из начальной точки
появляется сильное расхождение RSSI по общим BSSID. Это подтверждает выбранный
подход: анализировать по временным окнам, device serial/model и BSSID, а не
считать дистанцию из одного абсолютного RSSI.

## Phase 2 Header

В следующий APK добавляется стартовая строка:

```text
FIELD_DIAG event=device_context manufacturer=... model=... sdk=...
batteryPercent=... charging=... powerSave=... wifiEnabled=...
locationEnabled=...
```

Это нужно, чтобы отделять радиокартину от состояния телефона: модель, Android,
заряд, зарядка, энергосбережение, включенный Wi-Fi и включенная геолокация.

Проверка Phase 2 выполнена 2026-05-19:

```text
R3CT20C8A8N: Samsung SM-S908B, SDK 36, battery 74, charging=false, powerSave=true
e089985a: OnePlus NE2215, SDK 35, battery 73, charging=true, powerSave=false
```

## Phase 2 Controlled Run 10:45

Перед прогоном diagnostics очищены на обоих телефонах. Оба телефона
разблокированы, подключены к питанию, Wi-Fi включен, геолокация включена,
энергосбережение отключено.

Логи:

```text
artifacts/field-logs/run-20260519-1045/R3CT20C8A8N/diagnostics/field-radio-20260519-104237.log
artifacts/field-logs/run-20260519-1045/e089985a/diagnostics/field-radio-20260519-104238.log
```

Analyzer:

```text
artifacts/field-analysis-run-20260519-1045/
artifacts/field-analysis-run-20260519-1045-named/
```

Маркеры перемещения Samsung:

```text
10:45:10 - положил на шкаф
10:47:10 - унес в коридор
10:49:10 - вернул на шкаф
10:51:10 - положил примерно на 30 см
```

Именованные окна analyzer:

```text
cabinet        => шкаф, 10:45:10..10:47:10
corridor       => коридор, 10:47:10..10:49:10
cabinet_return => возврат на шкаф, 10:49:10..10:51:10
near_30cm      => около 30 см, после 10:51:10
```

Итог:

```text
Scan entries: 9352
R3CT20C8A8N: Samsung SM-S908B, battery 71, charging=true, powerSave=false
e089985a: OnePlus NE2215, battery 72, charging=true, powerSave=false
```

Во всех окнах были свежие receiver results, отклоненных scan request не было.
Самые сильные movement candidates на Samsung показали изменение RSSI по общим
офисным BSSID примерно на 25-28 dB.

Offline zone evaluator:

```text
artifacts/field-analysis-run-20260519-1045-zone-eval/
```

Результат:

```text
30-second bucket predictions: 29/32 correct (90.6%)
```

Три ошибки пришлись на buckets рядом с границами перемещения. Для ручных
переходов и 30-секундных buckets это ожидаемо.
