# 2026-05-21 Three Phone IFF Office BLE Run

Дата: 2026-05-21.

## Роли

Фактическая раскладка трех телефонов:

```text
OnePlus e089985a - Вася - PHONE_A_WITNESS - неподвижный свидетель A.
Samsung R3CT20C8A8N - Женя - PHONE_B_WITNESS - неподвижный свидетель B.
MI MAX 83efb856 - Петя - PHONE_C_MOVING_TARGET - движущаяся цель C.
```

Перед стартом все три устройства были в IFF, `RADIO ON`.

## Артефакты

```text
raw logs:
artifacts/field-logs-20260521-three-phone/

selected analyzer input:
artifacts/field-logs-20260521-three-phone-selected/

analysis:
artifacts/field-analysis-20260521-three-phone/summary.md
artifacts/field-analysis-20260521-three-phone/iff-field-checks.csv
artifacts/field-analysis-20260521-three-phone/iff-field-check-summary.csv
```

Analyzer windows:

```text
near_a  11:31:42..11:32:17
middle  11:32:17..11:33:09
near_b  11:33:09..11:34:29
decay   11:34:29..11:35:10
```

## Что подтверждено

Трехтелефонная BLE IFF-схема работает на уровне одновременного radio claim:

- A/Вася и B/Женя одновременно пишут BLE-прием от C/Пети.
- C/Петя тоже видит двух свидетелей: перед стартом UI показывал `current 2`, `radio 2/2`.
- Office roles корректно пишутся в UI и field checks: `PHONE_A_WITNESS`, `PHONE_B_WITNESS`, `PHONE_C_MOVING_TARGET`.
- Оба witness-телефона сделали финальный `field_check` с `local=vasya rx petya -47dBm` и `local=zhenya rx petya -56dBm`.

BLE RSSI по `playerId=petya` в окне теста:

```text
OnePlus A: 215 samples, avg -53.4 dBm, min -94, max -32.
Samsung B: 1374 samples total, 584 valid after removing rssi=127 outliers,
           avg -48.7 dBm, min -124, max -7.
```

По временным окнам:

```text
near_a:
  A sees Petya: 40 valid samples, avg -42.5 dBm.
  B sees Petya: 110 valid samples, avg -32.9 dBm, plus 148 rssi=127 outliers.

middle:
  A sees Petya: 64 valid samples, avg -43.9 dBm.
  B sees Petya: 158 valid samples, avg -31.6 dBm, plus 231 rssi=127 outliers.

near_b:
  A sees Petya: 77 valid samples, avg -70.2 dBm.
  B sees Petya: 211 valid samples, avg -67.1 dBm, plus 266 rssi=127 outliers.

decay:
  A sees Petya: 34 valid samples, avg -45.8 dBm.
  B sees Petya: 105 valid samples, avg -54.3 dBm, plus 145 rssi=127 outliers.
```

## Ограничения результата

Этот прогон не подтвердил чистый `radio off -> stale -> unknown` lifecycle для C:
в финальном окне `decay` A/B все еще принимали `BLE_IFF_PETYA`. MI MAX пропадал
из ADB во время отхода, но BLE-реклама продолжалась.

Samsung дает много BLE RSSI выбросов `127`. Для анализа близости такие значения
надо фильтровать, иначе они ломают среднее и пороги.

Геометрия "Петя ближе к A/B" в этом прогоне неоднозначна. По BLE RSSI C был
сильным на обоих witness-телефонах в первых двух окнах, а окно `near_b`
получилось слабым на обоих. Это похоже либо на фактическую траекторию/экранирование,
либо на нестабильность BLE RSSI, либо на слишком короткие и неточно размеченные
окна.

Wi-Fi fingerprint по окнам слабый:

```text
Best-zone bucket accuracy: 20/41, 48.8%.
Leave-one-bucket-out accuracy: 11/41, 26.8%.
```

Это подтверждает прежний вывод: Wi-Fi полезен как фон/контекст, но не как
главный слой IFF-близости.

## Вывод

Прогон успешен как проверка трех ролей и одновременного BLE radio presence:

```text
A witness + B witness + moving target C работают вместе.
Все три устройства могут одновременно рекламировать и сканировать BLE IFF.
Логи достаточно богаты для post-run анализа.
```

Прогон частично неуспешен как проверка направленной близости и decay:

```text
Нельзя честно сказать, что текущая логика уже определяет "Петя ближе к A или B".
Нельзя зачесть финальное окно как исчезновение Пети: BLE продолжал приниматься.
```

## Влияние на следующий шаг

Нужен следующий срез не "еще один общий прогон", а более контролируемый BLE
geometry test:

```text
1. A и B разнести дальше и зафиксировать физические точки.
2. C держать по 60 секунд строго рядом с A, в середине, рядом с B.
3. Для decay выключать RADIO ON на C или force-stop приложения на C, а не просто отходить.
4. В analyzer добавить отдельную BLE RSSI summary по playerId/window/device и фильтр rssi=127.
5. UI не должен делать direction claim по одному RSSI; пока только "radio presence" и "coarse proximity".
```

