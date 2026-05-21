# 2026-05-21 Separated Phone IFF Office BLE Repeat

Дата: 2026-05-21.

## Окно

```text
start:  2026-05-21 12:16:25
finish: 2026-05-21 12:19:20
```

Сценарий: телефоны физически разнесены; пользователь стоял около ключевых
точек примерно по 15 секунд и ходил с Samsung.

## Артефакты

```text
raw selected logs:
artifacts/field-logs-20260521-separated-repeat/

analysis:
artifacts/field-analysis-20260521-separated-repeat/summary.md
artifacts/field-analysis-20260521-separated-repeat/ble-rssi-15s.csv
artifacts/field-analysis-20260521-separated-repeat/ble-rssi-events.csv
```

Стандартный analyzer был запущен с окнами `p00..p11`, по 15 секунд каждое.

## Важная оговорка по ролям

По логам роли остались прежними:

```text
OnePlus e089985a - localPlayerId=vasya.
Samsung R3CT20C8A8N - localPlayerId=zhenya.
MI MAX 83efb856 - localPlayerId=petya.
```

То есть физически ходил Samsung/Женя, а не Петя. Этот прогон полезен как
BLE-geometry repeat, но его надо читать как движение `zhenya` относительно
стационарного `petya`, а не как движение `petya`.

## Качество данных

OnePlus в окне `12:16:25..12:19:20` не дал `IFF_DIAG event=ble_field_radio_rx`.
Он продолжал писать Wi-Fi/sensor events, но BLE IFF-событий в этом окне нет.
После прогона OnePlus был на lock screen. Поэтому этот repeat не является
полным A/B witness тестом.

MI MAX и Samsung дали BLE IFF-события. Samsung по-прежнему иногда пишет
`rssi=127`; эти значения в `ble-rssi-15s.csv` учтены как outliers и не входят
в `AvgRssi`.

## BLE RSSI по 15-секундным окнам

Ключевая полезная линия: `MI MAX / petya` видит `Samsung / zhenya`.

```text
p00  0..15s    zhenya at MI: avg -83.2 dBm
p01  15..30s   zhenya at MI: avg -81.2 dBm
p02  30..45s   zhenya at MI: avg -79.7 dBm
p03  45..60s   zhenya at MI: avg -72.0 dBm
p04  60..75s   zhenya at MI: avg -92.8 dBm
p05  75..90s   zhenya at MI: avg -68.3 dBm
p06  90..105s  zhenya at MI: avg -75.7 dBm
p07  105..120s zhenya at MI: avg -81.8 dBm
p08  120..135s zhenya at MI: avg -55.2 dBm
p09  135..150s zhenya at MI: avg -49.5 dBm
p10  150..165s zhenya at MI: avg -56.9 dBm
p11  165..180s zhenya at MI: avg -43.8 dBm
```

Это уже похоже на физическое сближение/удаление: первые окна слабые, затем
есть сильное сближение в `p08..p11`.

Samsung одновременно видел `petya` так:

```text
p00 -72.7 dBm
p01 -68.3 dBm
p02 -61.0 dBm
p03 -66.0 dBm
p04 -70.5 dBm
p05 -54.0 dBm
p06 -73.0 dBm
p07 -60.6 dBm
p08 -39.5 dBm
p09 -50.2 dBm
p10 -44.5 dBm
p11 -32.5 dBm
```

Самая сильная близость Samsung к MI MAX видна в конце окна:

```text
p08..p11: примерно -55 -> -44 dBm на MI MAX,
          примерно -39 -> -32 dBm на Samsung.
```

## Wi-Fi fingerprint

Wi-Fi по 15-секундным окнам снова не годится как надежный классификатор:

```text
Best-zone bucket accuracy: 6/29, 20.7%.
Leave-one-bucket-out accuracy: 0/29, 0%.
```

Это ожидаемо: окна слишком короткие, точки близкие, а Wi-Fi шумный.

## Вывод

Повтор подтвердил, что при физическом движении одного телефона BLE RSSI меняется
сильно и заметно. Особенно хорошо это видно на паре `MI MAX petya` <->
`Samsung zhenya`.

Но этот repeat не закрывает задачу `A/B witnesses decide which side is closer`,
потому что:

```text
OnePlus не дал BLE IFF events в окне эксперимента.
Физически двигался Samsung/zhenya, а не phone C / petya.
Ручных временных отметок точек не было, только общий старт/финиш.
```

Практический следующий шаг:

```text
1. Перед прогоном открыть IFF на всех трех телефонах и снять UI dump.
2. Убедиться, что A и B оба пишут ble_field_radio_rx до старта.
3. Физически ходить с тем телефоном, который в логах localPlayerId=petya.
4. После каждой точки нажимать ЗАПИСАТЬ или хотя бы дать голосовую/текстовую отметку.
5. В analyzer встроить BLE RSSI 15s summary штатно, включая фильтр rssi=127.
```

