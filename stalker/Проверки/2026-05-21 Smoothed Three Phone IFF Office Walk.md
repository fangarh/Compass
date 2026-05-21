# 2026-05-21 Smoothed Three Phone IFF Office Walk

Дата: 2026-05-21.

## Окно

```text
start marker: 2026-05-21 16:59:26 +03:00
finish pull:   2026-05-21 17:02:51 +03:00
```

Сценарий по факту:

```text
1. Samsung оставался рядом с OnePlus, пока пользователь относил MI.
2. Затем пользователь прошел с Samsung.
3. В конце Samsung снова был рядом с OnePlus, пока пользователь возвращал MI.
```

## Устройства и роли

```text
OnePlus e089985a      - Вася  / PHONE_A_WITNESS
MI MAX 83efb856       - Женя  / PHONE_B_WITNESS
Samsung R3CT20C8A8N   - Петя  / PHONE_C_MOVING_TARGET
```

Перед стартом все три UI были проверены в IFF и показывали `RADIO ON`.

## Артефакты

```text
raw logs:
artifacts/field-logs-20260521-smoothed-walk/

selected logs:
artifacts/field-logs-20260521-smoothed-walk-selected/

analysis:
artifacts/field-analysis-20260521-smoothed-walk/summary.md
artifacts/field-analysis-20260521-smoothed-walk/office-proximity-verdict.csv
artifacts/field-analysis-20260521-smoothed-walk/ble-rssi-summary.csv
artifacts/field-analysis-20260521-smoothed-walk/iff-field-checks.csv
```

Analyzer был запущен с окном:

```text
smoothed_walk=2026-05-21 16:59:26..2026-05-21 17:02:51
BucketSeconds=15
```

## Office proximity verdict

Разбивка внутри окна `smoothed_walk`:

```text
CLOSER_TO_B              7 buckets
BETWEEN_OR_AMBIGUOUS     5 buckets
CLOSER_TO_A              2 buckets
```

Подробная шкала:

| Bucket | A/Vasya RSSI | B/Zhenya RSSI | Delta dB | Verdict |
| --- | ---: | ---: | ---: | --- |
| 16:59:26-16:59:41 | -61.2 | -47.5 | -13.7 | CLOSER_TO_B |
| 16:59:41-16:59:56 | -60.0 | -47.4 | -12.6 | CLOSER_TO_B |
| 16:59:56-17:00:11 | -60.2 | -47.7 | -12.5 | CLOSER_TO_B |
| 17:00:11-17:00:26 | -59.8 | -48.3 | -11.5 | CLOSER_TO_B |
| 17:00:26-17:00:41 | -59.1 | -61.8 | 2.7 | BETWEEN_OR_AMBIGUOUS |
| 17:00:41-17:00:56 | -60.1 | -70.5 | 10.4 | CLOSER_TO_A |
| 17:00:56-17:01:11 | -73.5 | -57.4 | -16.1 | CLOSER_TO_B |
| 17:01:11-17:01:26 | -72.3 | -62.4 | -9.9 | CLOSER_TO_B |
| 17:01:26-17:01:41 | -79.2 | -76.6 | -2.6 | BETWEEN_OR_AMBIGUOUS |
| 17:01:41-17:01:56 | -75.2 | -60.8 | -14.4 | CLOSER_TO_B |
| 17:01:56-17:02:11 | -66.1 | -67.4 | 1.3 | BETWEEN_OR_AMBIGUOUS |
| 17:02:11-17:02:26 | -44.9 | -70.1 | 25.2 | CLOSER_TO_A |
| 17:02:26-17:02:41 | -42.7 | -43.8 | 1.1 | BETWEEN_OR_AMBIGUOUS |
| 17:02:41-17:02:56 | -40.1 | -35.6 | -4.5 | BETWEEN_OR_AMBIGUOUS |

## Качество данных

```text
Samsung/Petya имел валидные samples по A и B в каждом 15-секундном bucket.
Фильтр rssi=127 сработал: у линии Samsung -> Zhenya было много outlier 127.
OnePlus был на 1% батареи, charging=true, powerSave=true.
Samsung тоже был powerSave=true.
MI Wi-Fi был off, но BLE IFF работал.
```

## Вывод

Сглаженный verdict технически работает: он стабильно формирует buckets,
фильтрует `rssi=127`, показывает переходы `CLOSER_TO_A`, `CLOSER_TO_B` и
`BETWEEN_OR_AMBIGUOUS`.

Но этот прогон показал важный аппаратный bias: даже когда Samsung физически
оставался рядом с OnePlus во время переноса MI, линия Samsung -> MI часто была
сильнее линии Samsung -> OnePlus. Вероятная причина - асимметрия железа/мощности
передачи и состояние OnePlus на 1% батареи с power-save.

Следующий инженерный шаг: добавить короткую калибровку перед verdict:

```text
baseline(C,A) while C is known near A
baseline(C,B) while C is known near B
normalized_delta = (C-A RSSI - baseline(C,A)) - (C-B RSSI - baseline(C,B))
```

Без такой нормализации грубый verdict полезен как сигнал изменения, но не как
абсолютная геометрическая истина между разными моделями телефонов.
