# 2026-05-21 Three Phone IFF Office Test Plan

Дата: 2026-05-21.

## Цель

Проверить трехтелефонный BLE IFF-сценарий в офисе:

```text
два неподвижных свидетеля слышат одного идущего игрока;
RSSI и freshness меняются честно;
старый сигнал уходит в STALE/UNKNOWN;
по данным можно обсуждать область/близость, но не точную точку и не азимут.
```

Это не тест Wi-Fi Direct и не тест полноценного witness-report exchange.
Ближайшая задача - собрать реальные BLE witness observations и понять, какие
данные нужны следующему срезу.

## Роли телефонов

Использовать roster-идентичности так:

```text
Телефон A - Вася - PHONE_A_WITNESS - неподвижный свидетель 1.
Телефон B - Женя - PHONE_B_WITNESS - неподвижный свидетель 2.
Телефон C - Петя - PHONE_C_MOVING_TARGET - идущий игрок / "я подхожу".
```

Если нужен операторский/резервный профиль:

```text
Вы - PHONE_OPERATOR.
```

В приложении:

- открыть `IFF`;
- на вкладке `КОМАНДА` долгим нажатием назначить `THIS DEVICE`;
- включить `RADIO ON`;
- на свидетелях A/B выбрать `Петя`;
- перед каждым шагом или сразу после него нажимать `ЗАПИСАТЬ` на свидетелях.

`field_check` теперь пишет:

```text
officeRole
selectedOfficeRole
localDevicePlayerId
playerId
combatState
proximityLabel
witnessFreshSources
witness RSSI/age/source
```

## Базовый сценарий

Расставить A и B неподвижно. Начать с 7 метров между ними.

```text
1. A и B неподвижны, C вне зоны или radio off.
2. C включает RADIO ON как Петя.
3. C подходит к A.
4. C стоит примерно между A и B.
5. C подходит к B.
6. C кладет телефон в карман.
7. C экранирует телефон телом.
8. C уходит или выключает RADIO OFF.
9. Дождаться STALE и UNKNOWN у свидетелей.
```

Если офис позволяет, повторить с A-B на 10-14 метров.

## Что записывать руками

Для каждого шага держать короткую метку времени:

```text
HH:MM A-B 7m / C near A
HH:MM A-B 7m / C middle
HH:MM A-B 7m / C near B
HH:MM pocket
HH:MM body shield
HH:MM C radio off
```

Эти метки потом передаются в analyzer через `-Windows`, например:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\analyze-field-logs.ps1 `
  -InputRoot artifacts\field-logs `
  -OutputDir artifacts\field-analysis-three-phone-20260521 `
  -Windows "near_a=11:10:00..11:12:00;middle=11:12:00..11:14:00;near_b=11:14:00..11:16:00;off=11:16:00..11:18:00" `
  -BucketSeconds 10 `
  -BeaconSsids "COMPASS_IFF*"
```

## Ожидаемая картина

Хороший результат:

```text
C ближе к A -> A видит Петю сильнее, чем B.
C между A и B -> RSSI у A/B ближе друг к другу.
C ближе к B -> B видит Петю сильнее, чем A.
C в кармане или за телом -> RSSI может резко просесть или стать нестабильным.
C выключен/ушел -> RADIO_FRESH -> RADIO_STALE -> UNKNOWN.
```

Плохой или спорный результат тоже полезен:

```text
A и B показывают одинаковый RSSI при разных расстояниях;
один телефон плохо сканирует BLE;
фоновые режимы держатся по-разному;
RSSI скачет сильнее, чем ожидается.
```

## Границы вывода

После этого теста можно говорить:

- какой свидетель слышал Петю свежее;
- у какого свидетеля RSSI был сильнее;
- как быстро сигнал стал stale/unknown;
- есть ли практический смысл считать область по свидетелям.

Нельзя говорить:

- точные координаты Пети;
- надежный азимут;
- криптографически доказанная identity;
- готовая witness-сеть между телефонами.

## Следующий срез после теста

Если RSSI-картина полезная:

```text
1. Спроектировать real witness-report exchange.
2. Ограничить exchange активным контактом и IFF-группой.
3. Добавить GPS payload и location confidence.
4. Добавить WITNESS_RADIO_GEOMETRY как fallback при плохом GPS цели.
```

Если RSSI-картина шумная:

```text
1. Сначала усилить диагностику и агрегацию RSSI/trend.
2. Не строить direction UI до повторяемой evidence-модели.
3. Оставить BLE как current freshness proof, а не как геометрию.
```
