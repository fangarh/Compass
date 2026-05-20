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

## Реализованный срез Phase 15

Повторили field run с несколькими замерами на каждое состояние:

```text
near / far / body-shielded / pocket / off -> стабильность порогов
```

Фактические состояния:

- near: 3 samples, `RADIO_NEAR 75%`, avg RSSI `-28.0 dBm`;
- far, офис 5-7 м + стены: 3 samples, `RADIO_MID 55%`, avg RSSI `-59.7 dBm`;
- return near: 2 samples, `RADIO_NEAR 75%`, avg RSSI `-21.0 dBm`;
- body-shielded: 3 samples, `RADIO_NEAR 75%`, avg RSSI `-45.3 dBm`;
- cabinet-shielded: 3 samples, `RADIO_NEAR 75%`, avg RSSI `-43.7 dBm`;
- off: `STALE_RADIO` примерно на `56s`, затем `UNKNOWN` после `75s+`.

Вывод: текущие runtime-пороги можно оставить для MVP как грубый
proximity-hint. Важнее свежесть: после выключения hotspot старый сильный RSSI
не должен подтверждать близость, и текущая модель это делает правильно.

## Следующий срез

## Реализованный срез Phase 16

Сделали более осторожный UI proximity до multi-witness:

```text
RADIO_NEAR / RADIO_MID -> RADIO_NEAR / RADIO_WEAK_HINT
```

Решение:

- RSSI-пороги не менялись;
- `RADIO_MID` переименован в `RADIO_WEAK_HINT`;
- `RADIO_WEAK` переименован в `RADIO_EDGE_HINT`;
- medium/edge scores снижены до `45%` и `30%`;
- summary команды теперь показывает `PROXIMITY STRONG`, а не `PROXIMITY OK`;
- сильным proximity считается только `RADIO_NEAR`.

Это сохраняет полевой вывод Phase 15: far через стены остается полезной
подсказкой, но не выглядит как точное расстояние или подтверждение близкого
контакта.

## Следующий срез

## Реализованный срез Phase 17

Добавлен первый multi-witness foundation:

```text
один заявленный игрок -> несколько телефонов слышат beacon -> confidence выше
```

Пока без сети. Текущий телефон считается `local-device`, а remote teammate
reports явно показываются как `PENDING`.

Что появилось:

- `WITNESSES: ...` на карточке контакта;
- блок `WITNESS QUORUM`;
- `MULTI-WITNESS: 0` на экране команды;
- quorum state на карте;
- поля в diagnostic log:
  - `witnessQuorum`;
  - `witnessFreshSources`;
  - `witnessPossibleSources`.

Важно: quorum в будущем может повышать proximity confidence, но не identity
confidence без криптографии. Direction также не появляется из RSSI/quorum.

## Следующий срез

## Реализованный срез Phase 18

Спроектирован локальный contract для remote witness reports:

```text
source player -> target player -> freshness/RSSI -> signature placeholder
```

Добавлены:

- `IffRemoteWitnessReport`;
- `IffRemoteWitnessStore`;
- contract version `iff-remote-witness-v1`;
- signature placeholder `SIGNATURE_PENDING`;
- UI `REMOTE REPORTS: 0`;
- поля в diagnostic log:
  - `remoteWitnessContract`;
  - `remoteReportCount`;
  - `remoteFreshSources`.

Analyzer теперь выводит remote contract поля в CSV/Markdown.

Важно: remote reports смогут повышать proximity quorum позже, но не identity
confidence без проверки подписи.

## Следующий срез

Добавить local-only simulation/fixture для remote witness reports:

```text
без сети -> вручную добавить synthetic remote report -> проверить MULTI_WITNESS
```

Это позволит отладить quorum UI до реального транспорта.

## Реализованный срез Phase 19

Добавлен local-only simulation/fixture для remote witness reports:

```text
выбранный участник -> SIM WITNESS -> два synthetic remote reports -> MULTI_WITNESS
```

Что появилось:

- кнопка `SIM WITNESS` на IFF-экране;
- два synthetic remote reports для выбранного контакта;
- contract `iff-remote-witness-v1`;
- signature status `SIGNATURE_PENDING`;
- `WITNESSES: MULTI_WITNESS 2/3` при свежих reports;
- `remote_witness_simulated` и remote witness counts в diagnostic log;
- analyzer видит `MULTI_WITNESS`, `remoteReportCount=2` и
  `remoteFreshSources=2`.

Проверка на OnePlus:

- `Main -> IFF -> Команда -> Петя -> SIM WITNESS -> Контакт`;
- контакт показал два свежих remote reports;
- identity осталась `ROSTER_ONLY`, без crypto upgrade;
- `ЗАПИСАТЬ` записал `MULTI_WITNESS 2/3`;
- analyzer по сессии `20260519-1716` подтвердил `2 reports / 2 fresh`.

Важно: это только fixture для UI/quorum/logging. Он не означает реальный
транспорт, криптографическую идентичность, направление или точную позицию.

## Следующий срез

Сделать freshness/expiry remote reports видимыми и управляемыми в local
fixture:

```text
fresh reports -> истечение freshness window -> stale/unknown transition
```

Это нужно, чтобы явно проверять переход `MULTI_WITNESS` обратно в
неподтвержденное состояние до подключения реального транспорта.

## Реализованный срез Phase 20

Сделали freshness/expiry remote reports проверяемыми без ожидания и без сети:

```text
SIM FRESH -> MULTI_WITNESS 2/3
SIM STALE -> STALE_REMOTE_WITNESS 0/3
```

Что изменилось:

- `SIM WITNESS` разделен на `SIM FRESH` и `SIM STALE`;
- `SIM FRESH` добавляет два fresh synthetic reports;
- `SIM STALE` заменяет их двумя stale reports;
- stale reports остаются видимыми, но не считаются current witness proof;
- diagnostic log пишет `remoteStaleSources`;
- analyzer показывает `reports / fresh / stale`.

Проверка на OnePlus:

- `Main -> IFF -> Команда -> Петя -> SIM FRESH -> SIM STALE -> Записать`;
- fresh state: `MULTI_WITNESS 2/3`;
- stale state: `STALE_REMOTE_WITNESS 0/3`;
- log: `remoteReportCount=2 remoteFreshSources=0 remoteStaleSources=2`;
- analyzer по сессии `20260519-1735` подтвердил
  `2 reports / 0 fresh / 2 stale`.

Важно: expired remote reports - это память о старом свидетельстве, а не
доказательство текущей близости. Identity/proximity/position/direction не
повышаются от stale reports.

## Следующий срез

Либо первый transport stub для remote witness exchange, либо компактный
operator view, который быстрее отделяет current witness evidence от stale
evidence в боевом UI.

## Реализованный срез Phase 21

Добавлен компактный operator view для witness evidence:

```text
current witness -> stale evidence -> no current evidence
```

Новые operator verdict labels:

- `CURRENT_MULTI_WITNESS`
- `CURRENT_SINGLE_WITNESS`
- `STALE_EVIDENCE_ONLY`
- `LOCAL_DECLARED_ONLY`
- `NO_CURRENT_EVIDENCE`

Что изменилось:

- карточка контакта сверху показывает `OPERATOR: ...`;
- блок `OPERATOR VIEW` показывает current/stale counts;
- команда показывает счетчики `CURRENT WITNESS` и `STALE EVIDENCE`;
- строки roster показывают operator verdict вместе с identity/proximity;
- diagnostic log пишет `operatorVerdict`;
- analyzer выводит operator verdict в CSV/Markdown.

Проверка на Samsung:

- `Main -> IFF -> Команда -> Петя -> SIM FRESH -> SIM STALE -> Записать`;
- fresh state: `OPERATOR: CURRENT_MULTI_WITNESS`;
- stale state: `OPERATOR: STALE_EVIDENCE_ONLY`;
- log: `operatorVerdict=STALE_EVIDENCE_ONLY`;
- analyzer по сессии `20260520-0914` подтвердил `STALE_EVIDENCE_ONLY`.

Важно: operator verdict - это сводка для боевого чтения UI, а не новый источник
доказательства. Identity/proximity/position/direction остаются отдельными
слоями.

## Следующий срез

Первый real transport stub для remote witness exchange. Когда второй телефон
зарядится, его можно использовать для проверки приема/передачи; до этого
transport points стоит держать локальными и хорошо логируемыми.
