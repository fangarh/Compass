# 2026-05-21 Two Phone IFF Office BLE Run

Дата: 2026-05-21.

## Роли

Фактически в прогоне участвовали два телефона:

```text
OnePlus e089985a - Вася - PHONE_A_WITNESS - неподвижный свидетель.
Samsung - Петя - PHONE_C_MOVING_TARGET - движущийся телефон.
```

Samsung во время прогона не был стабильно виден по ADB, поэтому все
проверяемые логи сняты с OnePlus.

## Артефакты

```text
raw log:
artifacts/field-logs-20260521-two-phone/e089985a/field-radio-20260521-093450.log

analysis:
artifacts/field-analysis-20260521-two-phone/summary.md
artifacts/field-analysis-20260521-two-phone/iff-field-checks.csv
artifacts/field-analysis-20260521-two-phone/iff-field-check-summary.csv
```

Analyzer windows:

```text
baseline   09:36:00..09:38:30
first_fresh 09:38:30..09:41:20
near       09:41:45..09:42:30
off_decay  09:43:40..09:45:10
```

## Что подтверждено

OnePlus как `PHONE_A_WITNESS` увидел Samsung-Петю как `BLE_IFF_PETYA`.

Baseline:

```text
09:36:30
combatState=UNKNOWN
proximityLabel=UNKNOWN
witness=none
```

Первое обнаружение / слабый контакт:

```text
09:41:04
combatState=CURRENT_SINGLE
proximityLabel=RADIO_EDGE_HINT
witness=RADIO_FRESH
RSSI=-71 dBm
ageMs=631
```

Рядом, телефоны положены близко:

```text
09:42:10
combatState=CURRENT_SINGLE
proximityLabel=RADIO_NEAR
witness=RADIO_FRESH
RSSI=-44 dBm
ageMs=789
```

После выключения/исчезновения Samsung radio:

```text
09:43:57
witness_freshness_transition
petya RADIO_FRESH -> RADIO_STALE
ageMs=15061
RSSI=-51

09:44:43
witness_freshness_transition
petya RADIO_STALE -> UNKNOWN
ageMs=61427
RSSI=-51
```

Финальный field check:

```text
09:44:56
combatState=UNKNOWN
proximityLabel=UNKNOWN
witness=UNKNOWN
ageMs=74453
```

## Вывод

Двухтелефонный офисный BLE IFF-прогон успешен как проверка current evidence:

- свежий BLE claim от `Петя` появляется у свидетеля `Вася`;
- RSSI меняется от слабого `-71/-72 dBm` до near `-44 dBm`;
- UI корректно переходит в `CURRENT_SINGLE`;
- после исчезновения сигнала witness честно проходит `RADIO_STALE` и затем `UNKNOWN`;
- stale/unknown не остается current contact.

Это все еще не direction, не точные координаты и не crypto identity.

## Влияние на следующий шаг

Третий телефон теперь нужен не для проверки самого BLE freshness lifecycle, а
для проверки witness geometry:

```text
A witness near/far относительно C;
B witness near/far относительно C;
сравнить RSSI A и B в одном временном окне;
посмотреть, можно ли честно сказать "Петя ближе к A/B".
```

Следующий практический срез:

```text
OnePlus - PHONE_A_WITNESS
третий телефон - PHONE_B_WITNESS
Samsung - PHONE_C_MOVING_TARGET

A/B стоят неподвижно.
Samsung движется: near A -> middle -> near B -> radio off.
```
