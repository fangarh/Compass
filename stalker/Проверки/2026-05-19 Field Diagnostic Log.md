# 2026-05-19 Field Diagnostic Log

## Цель

Добавить полевой диагностический лог на телефоне для Wi-Fi/radio тестов.
Лог должен сохраняться в файл на устройстве и забираться после теста через ADB.

## Устройство

- `R3CT20C8A8N`
- Samsung SM-S908B / S22 Ultra

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
