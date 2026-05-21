# 2026-05-21 Clean Three Phone IFF Office BLE Repeat

Дата: 2026-05-21.

## Окно

```text
start:  2026-05-21 12:41:47 +03:00
finish: 2026-05-21 12:45:51 +03:00
```

Сценарий:

```text
1. Перед стартом роли были переподготовлены и проверены по UI.
2. Пользователь сначала отнес MI MAX.
3. Затем пользователь совершил проход с Samsung.
4. После прохода пользователь забрал MI MAX.
```

Физически движущимся телефоном в основном проходе был Samsung.

## Устройства и роли

```text
OnePlus e089985a      - Вася  / PHONE_A_WITNESS
MI MAX 83efb856       - Женя  / PHONE_B_WITNESS
Samsung R3CT20C8A8N   - Петя  / PHONE_C_MOVING_TARGET
```

Перед стартом все три телефона были открыты в IFF и показывали
`FIELD RADIO: ON`.

## Артефакты

```text
raw logs:
artifacts/field-logs-20260521-clean-repeat/

analysis:
artifacts/field-analysis-20260521-clean-repeat/summary.md
artifacts/field-analysis-20260521-clean-repeat/ble-rssi-summary.csv
artifacts/field-analysis-20260521-clean-repeat/ble-rssi-timeline.csv

retro verdict analysis:
artifacts/field-analysis-20260521-clean-repeat-retro-verdict/summary.md
artifacts/field-analysis-20260521-clean-repeat-retro-verdict/office-proximity-verdict.csv
```

Analyzer был запущен с окном `clean_repeat=12:41:47..12:45:51` и
15-секундными buckets.

После добавления ретроспективного office-verdict analyzer был повторно
запущен в `artifacts/field-analysis-20260521-clean-repeat-retro-verdict/`.

## Качество данных

Этот прогон закрывает два дефекта предыдущего repeat:

```text
1. Роли соответствуют плану A/B/C.
2. Все три телефона дали BLE IFF-события в окне эксперимента.
```

Ограничения:

```text
OnePlus был на 14% батареи и в power-save mode.
Samsung продолжает давать rssi=127 выбросы, особенно по линии petya -> zhenya.
У точек нет ручных меток, есть только старт/финиш и 15-секундные buckets.
```

Analyzer исключает `rssi=127` из Avg/Min/Max.

## BLE RSSI общий итог

Взвешенное среднее по всему окну `clean_repeat`:

| Device | Local | Seen | Valid | Outlier 127 | Avg RSSI |
| --- | --- | --- | ---: | ---: | ---: |
| MI MAX | zhenya | petya | 160 | 0 | -69.5 |
| MI MAX | zhenya | vasya | 151 | 0 | -63.7 |
| OnePlus | vasya | petya | 989 | 0 | -62.0 |
| OnePlus | vasya | zhenya | 1441 | 0 | -68.8 |
| Samsung | petya | vasya | 788 | 10 | -49.2 |
| Samsung | petya | zhenya | 1222 | 336 | -61.2 |

## Движение Samsung / Petya

Ключевая линия для moving target: Samsung/Petya относительно OnePlus/Vasya.

```text
12:41:47-12:42:17  Samsung видел Vasya очень сильно: ~ -33 dBm.
12:42:32-12:43:17  Samsung/Vasya ушел в слабую зону: ~ -74 .. -84 dBm.
12:43:47-12:44:47  Samsung снова приближался к Vasya: ~ -49 .. -56 dBm.
12:44:47-12:45:47  Samsung снова рядом с Vasya: ~ -36 .. -32 dBm.
```

Это симметрично подтверждается OnePlus:

```text
OnePlus видел Petya:
12:41:47-12:42:17  ~ -40 dBm
12:42:32-12:43:17  ~ -85 .. -94 dBm
12:44:47-12:45:47  ~ -48 .. -41 dBm
```

То есть BLE уверенно поймал уход Samsung от OnePlus и возврат к нему.

## Линия MI MAX / Zhenya

MI MAX был вынесен отдельно до основного прохода, поэтому его линия читается как
стационарная/перенесенная точка B, а не как синхронный witness с ручной меткой.

```text
MI MAX видел Petya:
12:41:47-12:42:02  -46.2 dBm
12:42:17-12:43:17  -83.8 .. -91.7 dBm
12:44:17-12:45:02  -79.6 .. -80.5 dBm
12:45:02-12:45:47  -61.3 .. -60.9 dBm
```

Samsung видел Zhenya:

```text
12:41:47-12:42:02  -34.3 dBm
12:42:02-12:43:17  -66.9 .. -79.6 dBm
12:45:02-12:45:47  -49.2 .. -50.3 dBm
```

Это согласуется с уточненным сценарием: MI сначала переносился, затем Samsung
ходил, затем MI был забран.

## Вывод

Прогон полезен как первый чистый A/B/C BLE repeat:

```text
Роли правильные.
Все три телефона пишут BLE.
RSSI хорошо отражает физическое сближение/удаление.
Samsung/Petya как moving target надежно виден OnePlus/Vasya.
MI MAX/Zhenya тоже участвует, но без ручных временных меток точек.
```

Ретроспективный `office-proximity-verdict.csv` по 15-секундным buckets
дал такую разбивку внутри окна `clean_repeat`:

```text
CLOSER_TO_A             11 buckets
CLOSER_TO_B              3 buckets
BETWEEN_OR_AMBIGUOUS     3 buckets
```

Это соответствует проходу: C/Petya был рядом с A/Vasya в начале и в конце,
а в середине были несколько окон ближе к B/Zhenya или между точками.

Главный инженерный вывод: BLE-слой уже достаточен, чтобы строить грубое
`near/mid/far` по парам телефонов. Для решения "к какой стороне ближе C" нужно
добавить в UI/анализ явную сравнительную метрику:

```text
distance_hint(C,A) vs distance_hint(C,B)
```

и считать ее по сглаженным 10-15 секундным BLE buckets, с фильтром `rssi=127`
и без попытки выводить направление/азимут из BLE.

## Следующий шаг

Сделать в приложении/operator summary отдельный office verdict:

```text
CLOSER_TO_A / CLOSER_TO_B / BETWEEN_OR_AMBIGUOUS / INSUFFICIENT_DATA
```

Основа для MVP:

```text
1. Брать последние fresh BLE RSSI A<->C и B<->C.
2. Сглаживать по короткому окну.
3. Игнорировать rssi=127.
4. Требовать минимальное количество samples.
5. Показывать verdict только если разница достаточно велика.
```

## Live office verdict smoke

После внедрения live office verdict Samsung был повторно подготовлен как
`petya / PHONE_C_MOVING_TARGET`, открыт в IFF и проверен по UI.

Артефакты короткого smoke:

```text
tail logs:
artifacts/field-logs-20260521-live-verdict-tail/

analysis:
artifacts/field-analysis-20260521-live-verdict-tail/summary.md
artifacts/field-analysis-20260521-live-verdict-tail/iff-field-checks.csv
artifacts/field-analysis-20260521-live-verdict-tail/office-proximity-verdict.csv
```

UI Samsung показывал:

```text
THIS DEVICE: Петя
OFFICE ROLE: PHONE_C_MOVING_TARGET
OFFICE VERDICT: CLOSER_TO_A delta=13dB
FIELD RADIO: ON / ble adv=on scan=on ...
```

Контрольный `field_check` на Samsung записал:

```text
officeProximityVerdict=CLOSER_TO_A
officeProximityDeltaDb=12
officeProximityReason="C hears A stronger than B by 12dB"
officeProximityA="RADIO_FRESH -43dBm 114ms"
officeProximityB="RADIO_FRESH -55dBm 159ms"
```

Ретро-анализ по 30-секундным buckets для этого короткого хвоста:

| Bucket | A RSSI | B RSSI | Delta | Verdict |
| --- | ---: | ---: | ---: | --- |
| 16:21:30-16:22:00 | -46.6 | -56.4 | 9.8 | CLOSER_TO_A |
| 16:22:00-16:22:30 | -47.9 | -55.6 | 7.7 | BETWEEN_OR_AMBIGUOUS |
| 16:22:30-16:23:00 | -46.5 | -54.1 | 7.6 | BETWEEN_OR_AMBIGUOUS |

Итог: live verdict работает на движущемся телефоне C/Petya и пишет в
`field_check`. Граница 8 dB выглядит разумной: явная точка проходит как
`CLOSER_TO_A`, а значения 7.6-7.7 dB остаются пограничными, без ложной
уверенности.

## Smoothed live office verdict smoke

После перехода live verdict с одиночного RSSI на 15-секундное rolling window
APK был пересобран и установлен на все три телефона. Samsung был разблокирован,
открыт в IFF, затем после накопления окна был записан контрольный `field_check`.

UI Samsung:

```text
THIS DEVICE: Петя
OFFICE ROLE: PHONE_C_MOVING_TARGET
OFFICE VERDICT: CLOSER_TO_B delta=-11dB
FIELD RADIO: ON / ble adv=on scan=on ...
```

Контрольный `field_check`:

```text
officeProximityVerdict=CLOSER_TO_B
officeProximityDeltaDb=-10
officeProximityReason="C hears B stronger than A by 10dB"
officeProximityA="RADIO_FRESH_WINDOW avg=-56dBm n=54 out127=0 newest=8ms"
officeProximityB="RADIO_FRESH_WINDOW avg=-46dBm n=84 out127=31 newest=17ms"
```

Итог: live UI и `field_check` теперь используют сглаженное окно, минимум samples
и явный счетчик отфильтрованных `rssi=127`.
