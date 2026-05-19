# 2026-05-19 MVP IFF Roadmap

## Поправка курса

Реальный MVP по обсуждению от 2026-05-18 - не поиск Wi-Fi-маяка/аномалии, а
тактический слой `свой-чужой`.

Минимальная формула:

```text
Кто-то рядом -> это заявленный свой? -> насколько уверенно? -> где примерно?
```

Wi-Fi/hotspot тесты остаются полезными, но только как технический слой свежего
радиосигнала и близости. Они не являются продуктовой целью MVP.

## Дорога к MVP

1. Отдельный экран IFF, не перегружающий основной PDA.
2. Локальный roster известных своих.
3. Доверенная идентичность игрока: ID/токен, а не имя телефона или SSID.
4. Ручной режим `я подхожу` с ограниченным временем действия.
5. Phone-to-phone сигнал подходящего игрока.
6. Свидетели: кто из своих слышит подходящего игрока.
7. Уверенность по независимым слоям:
   - идентичность;
   - близость;
   - позиция;
   - направление.
8. Экран `Команда` с приоритетной сортировкой.
9. Экран `Контакт` для быстрого боевого ответа.
10. Экран `Карта` с ошибкой GPS и связями свидетелей.
11. Полевой MVP-тест: один подходящий игрок, два свидетеля, плохой/хороший GPS.

## Первый реализованный срез

Phase 9 добавляет:

- кнопку `IFF` на основной PDA;
- отдельную форму `IffActivity`;
- вкладки `КОНТАКТ`, `КОМАНДА`, `КАРТА`;
- локальный режим `Я ПОДХОЖУ`;
- честные placeholder-состояния: roster не настроен, свидетелей нет,
  радиоподтверждения нет.

Этот срез не выдает ложное подтверждение своего. Он только создает контейнер,
в который дальше будут добавляться roster, доверенная идентичность и
phone-to-phone witnesses.

## Реализованный срез Phase 10

Добавлен локальный roster:

- `Вы`
- `Петя`
- `Вася`
- `Женя`

Экран `Команда` теперь показывает список своих, выбор участника открывает
`Контакт`, а `Я ПОДХОЖУ` относится только к локальному игроку `Вы`.

Важно: roster не доказывает близость. До phone-to-phone обмена proximity,
position и direction остаются `UNKNOWN`.

## Реализованный срез Phase 11

Добавить первый phone-to-phone witness через Wi-Fi scan:

```text
known beacon SSID
radio freshness
RSSI
signal age from ScanResult timestamp
rough proximity
```

Паттерн beacon:

- `COMPASS_IFF_YOU`
- `COMPASS_IFF_PETYA`
- `COMPASS_IFF_VASYA`
- `COMPASS_IFF_ZHENYA`

Это не криптографическая идентичность. Такой beacon доказывает только то, что
рядом недавно был слышен SSID, заявляющий известного участника. Поэтому
`identity`, `proximity`, `position` и `direction` остаются раздельными.

Реализация подключена к существующему 1 Hz Wi-Fi scan pipeline. Если beacon
не слышен, UI остается в честном состоянии `UNKNOWN`.

Полевой двухтелефонный smoke-test прошел:

- Samsung поднял hotspot `COMPASS_IFF_PETYA`;
- OnePlus на `КОМАНДА` увидел `RADIO FRESH: 1`;
- карточка `Петя` показала `RADIO_NEAR`, RSSI `-55 dBm`, age `1s`,
  frequency `2462 MHz`;
- после выключения hotspot witness перешел в stale примерно на 20 секунде и в
  `UNKNOWN` примерно на 60 секунде.

## Реализованный срез Phase 12

Сформализовать confidence model:

```text
identity: roster / radio claim / crypto later
proximity: fresh RSSI witness / stale / unknown
position: GPS point + error circle / unknown
direction: witness geometry later / unknown
```

После этого UI должен показывать не просто сырые состояния, а понятный уровень
уверенности по каждому независимому слою.

Текущая минимальная модель:

- `identity`: локальный self/roster/radio claim, без crypto;
- `proximity`: свежесть и RSSI beacon, либо stale/unknown;
- `position`: пока `UNKNOWN 0%`;
- `direction`: пока `UNKNOWN 0%`.

Главное правило: высокий proximity score не повышает direction или GPS
position. Это отдельные вопросы.

Проверка на OnePlus:

- без активного beacon `Петя` получает `ROSTER_ONLY 40%` и proximity
  `UNKNOWN 0%`;
- `Я ПОДХОЖУ` дает локальному игроку `LOCAL_SELF_APPROACH 80%`, но proximity
  остается `LOCAL_DECLARED_UNKNOWN 20%`;
- position и direction остаются `UNKNOWN 0%`.

## Реализованный срез Phase 13

Phase 13 делает field MVP test flow:

```text
2+ участника -> один beacon -> один receiver -> confidence UI -> журнал результата
```

Цель - проверить, что UI помогает принять боевое решение без ложного обещания
направления или криптографической идентичности.

Минимальный flow:

1. Открыть `IFF`.
2. Выбрать участника.
3. Посмотреть confidence layers.
4. Нажать `ЗАПИСАТЬ`.
5. Получить `IFF_DIAG event=field_check` в diagnostic log.

Это даст воспроизводимый журнал проверок для дальнейших полевых прогонов.

Проверка на OnePlus:

- выбран `Петя`;
- нажата кнопка `ЗАПИСАТЬ`;
- UI показал последнюю запись: `Петя: identity 40% / proximity 0% / witness none`;
- diagnostic log получил `IFF_DIAG event=field_check`.

Near/far/off field session:

- near: Samsung hotspot `COMPASS_IFF_PETYA`, OnePlus receiver,
  `RADIO_NEAR`, proximity `75%`, RSSI `-39 dBm`;
- far: `RADIO_MID`, proximity `55%`, RSSI `-68 dBm`;
- off/stale: `STALE_RADIO`, proximity `25%`, witness age about `29s`;
- off/unknown: `UNKNOWN`, proximity `0%`, witness age about `71s`.

## Реализованный срез Phase 14

Разобран near/far/off diagnostic log:

```text
near/far/off records -> field report -> threshold notes
```

Анализатор теперь понимает `IFF_DIAG event=field_check`, пишет
`iff-field-checks.csv`, `iff-field-check-summary.csv` и добавляет секцию
`IFF Field Checks` в `summary.md`.

Полевой вывод:

- near: `RADIO_NEAR 75%`, RSSI `-39 dBm`, age `2269 ms`;
- far: `RADIO_MID 55%`, RSSI `-68 dBm`, age `12161 ms`;
- off/stale: `STALE_RADIO 25%`, age `28759 ms`;
- off/unknown: `UNKNOWN 0%`, age `70886 ms`.

Текущие пороги годятся как грубая MVP-подсказка близости, но не как азимут,
позиция или криптографическое подтверждение. RSSI не должен повышать
`direction` или `position`.

## Следующий срез

Повторить field run с несколькими замерами на каждое состояние:

```text
near / far / body-shielded / pocket / off -> стабильность порогов
```

После этого можно осторожно решать, менять ли runtime-пороги
`RADIO_NEAR/MID/WEAK` или оставить текущие до появления второго свидетеля.
