# CointCore

Серверный coremod для модпака **COINT 1.2\*** на **Minecraft 1.21.1** / **NeoForge**.

Мод объединяет модерацию, чат, автоматизацию сервера, защиту чанков и интеграции с tech-модами в одном JAR. Устанавливается **только на dedicated server** — клиентам он не нужен.

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.228+ |
| Java | 21 |
| Mod ID | `cointcore` |

## Содержание

- [Возможности](#возможности)
- [Установка](#установка)
- [Сборка из исходников](#сборка-из-исходников)
- [Команды](#команды)
- [InvSee](#invsee)
- [Права доступа](#права-доступа)
- [Конфигурация](#конфигурация)
- [Tick watchdog](#tick-watchdog)
- [Интеграции](#интеграции)
- [Структура проекта](#структура-проекта)

## Возможности

### Модерация и социальные функции

- Личные сообщения (`/msg`, `/r`) и игнор-лист
- Муты, баны, варны и история наказаний
- Невидимость для игроков и мобов (`/vanish`, `/v`)
- Слежка за чатом (`/spy`)
- Админ-чат (`/a`, `/ac`, `/adminchat`)
- Телепорт к офлайн-игроку (`/tpl`)
- Просмотр и правка инвентарей (`/invsee`) — онлайн и офлайн, с журналом действий

### Серверная автоматизация

- Плановый и ручной рестарт (`/cointcore restart`)
- Периодические сообщения в чат
- Очистка предметов на земле (`world-cleanup`)
- Очистка мобов по расписанию (`mob-cleanup`)
- Голосования за день и ясную погоду (`/voteday`, `/votesun`)
- Интеграция со Spark (TPS/MSPT, профилирование)
- Tick watchdog: координаты дорогих block entity / entity и топ методов при лагах

### Защита территории (FTB Chunks)

- Расширенные флаги чанков команд (`/claim flag`)
- Перехват ломания/размещения блоков через mixin для:
  - Create (контрапции, deployer, super glue)
  - QuarryPlus, AE2, Ars Nouveau
  - ComputerCraft, Industrial Foregoing
  - Draconic Evolution, Actually Additions, Entangled

### Игровая механика

- Персональный PvP-режим (`/turn-pvp`)
- Keep Inventory с поддержкой Curios и Accessories
- Лимиты блоков и сущностей на чанк (`/chunklimit`)
- Лимиты и перехват лута спавнеров (Apothic Spawners)
- Кредиты на киты FTB Essentials (`/kit balance`, `/cointcore kit ...`)
- Уникальные фильтры ME (AE2)
- Донорские привилегии (полёт и др.)

## Установка

1. Скачайте или соберите `cointcore-1.0.0.jar`.
2. Положите JAR в папку `mods/` **только на сервере**.
3. Запустите сервер — конфиги создадутся автоматически в `config/cointcore/`.
4. Настройте права через LuckPerms или OP-уровень (см. [Права доступа](#права-доступа)).

> **Важно:** мод рассчитан на конкретный состав модпака COINT. На сервере без FTB Chunks, AE2, Create и других зависимых модов часть функций просто не активируется — mixin подключаются условно.

## Сборка из исходников

### Требования

- JDK 21
- Git

Локальная папка `server-mods` **не нужна** для компиляции. JAR сторонних модов под mixin подтягиваются из Maven (Curse Maven / официальные репозитории авторов).

### Шаги

```bash
git clone https://github.com/cubeofint/cointcore.git
cd cointcore
./gradlew build
```

Готовый JAR: `build/libs/cointcore-1.0.0.jar`.

CI на GitHub Actions собирает тот же `./gradlew build` на push в `main` и на pull request.

### Зависимости модпака (ATM10 8.2)

Версии compile-only модов зафиксированы в `gradle.properties` как координаты Curse Maven:

`curse.maven:<slug>-<projectId>:<fileId>`

Они совпадают с манифестом клиентского пака **All the Mods 10 8.2** (CurseForge project `925200`, file `8945086`), кроме:

| Источник | Моды |
|----------|------|
| Maven FTB (`maven.ftb.dev`) | FTB Essentials, Library, Teams, Chunks |
| Maven Illusive Soulworks | Curios API |
| Maven Wisp Forest | Accessories |
| Maven Central | LuckPerms API |
| Curse Maven, не из манифеста клиента 8.2 | TAB (`tab-1232967:7659430`, 1.21.1), Discord & Chat Images (`discord-chat-connect-1198238:8768897`) |

Репозитории в `build.gradle` ограничены через `content { includeGroup ... }`.

### Папка `server-mods`

Папка в `.gitignore` и **не нужна для сборки**. Туда можно вручную положить JAR модпака, если нужно разбирать байткод (декомпиляция, проверка mixin). В репозиторий их коммитить нельзя (лицензии сторонних модов).

### Как добавить зависимость под новый mixin

1. Найдите мод в манифесте ATM10 8.2 (`projectID` / `fileID`) или на CurseForge/Modrinth.
2. Добавьте координату в `gradle.properties` (`curse_<id>=<slug>-<projectId>:<fileId>`).
3. Добавьте `packMods(curseMod(curse_<id>)) { transitive = false }` в `build.gradle`.
4. Если мода нет на Curse Maven (автор запретил сторонние загрузки) — используйте `maven.modrinth:<slug>:<version>` или официальный Maven автора.
5. Если нет ни одного Maven: цели mixin через `@Mixin(targets = "...")` и `@Pseudo`, без импорта классов мода; либо минимальные заглушки в отдельном source set только для компиляции (не попадают в итоговый JAR).
6. Проверьте `./gradlew build` **без** папки `server-mods`.

### Запуск dev-сервера

```bash
./gradlew runServer
```

## Команды

### Модерация

| Команда | Описание |
|---------|----------|
| `/msg <игрок> <текст>` | Личное сообщение (алиасы: `m`, `w`, `tell`, `whisper`) |
| `/r <текст>` | Ответ на последнее ЛС |
| `/ignore <игрок>` | Игнор-лист |
| `/mute <игрок> <время> [причина]` | Замутить игрока |
| `/unmute <игрок>` | Снять мут |
| `/ban <игрок> <время> [причина]` | Забанить игрока |
| `/pardon <игрок>` | Разбанить |
| `/warn <игрок> <причина>` | Выдать предупреждение |
| `/punishments <игрок>` | История наказаний |
| `/tpl <игрок>` | Телепорт к офлайн-игроку |
| `/invsee <игрок> [раздел]` | Просмотр/правка инвентаря (см. [InvSee](#invsee)) |
| `/spy` | Вкл/выкл слежку за чатом |
| `/a <текст>` | Админ-чат (алиасы: `ac`, `adminchat`) |

### Игрок

| Команда | Описание |
|---------|----------|
| `/vanish` / `/v` | Невидимость |
| `/vanish mobs` / `/v mobs` | Скрытие от мобов |
| `/nv` | Ночное зрение |
| `/turn-pvp` | Переключить личный PvP-режим |
| `/voteday` | Голосование за день |
| `/votesun` / `/voteclearweather` | Голосование за ясную погоду |
| `/kit balance` | Баланс кредитов на киты (FTB Essentials) |

### Администрирование (`/cointcore`)

| Команда | Описание |
|---------|----------|
| `/cointcore reload` | Перезагрузить конфиги |
| `/cointcore restart [секунды]` | Запланировать рестарт (`cancel` — отменить) |
| `/cointcore worldcleanup status` | Статус очистки мира |
| `/cointcore worldcleanup run` | Запустить очистку вручную |
| `/cointcore chunklimit ...` | Управление лимитами чанков |
| `/cointcore claim flag ...` | Флаги чанков FTB (если FTB Chunks установлен) |
| `/cointcore kit ...` | Управление кредитами на киты (если FTB Essentials установлен) |
| `/cointcore watchdog ...` | Tick watchdog: отчёт, топ, телепорт к виновнику |

#### Флаги чанков (`/cointcore claim flag`)

| Подкоманда | Описание |
|------------|----------|
| `info <команда\|игрок>` | Показать флаги команды |
| `no_player_damage <true\|false>` | Запрет урона игрокам в чанке |
| `no_hostile_mob_spawn <true\|false>` | Запрет спавна враждебных мобов |
| `protect_mobs <true\|false>` | Защита мобов от игроков |

## InvSee

Просмотр и правка чужих инвентарей. Интерфейс пока прежний (отдельное окно на каждый раздел); вкладки в ванильном стиле будут отдельным обновлением.

### Команды

| Команда | Раздел |
|---------|--------|
| `/invsee <ник\|uuid>` | Инвентарь, броня, вторая рука |
| `/invsee <ник> ender` | Эндер-сундук |
| `/invsee <ник> curios` | Curios |
| `/invsee <ник> cosmetic` | Косметическая броня |
| `/invsee <ник> backpack [ключ\|номер]` | Sophisticated Backpacks |
| `/invsee <ник> pocket [uuid]` | Pocket Storage |
| `/invsee <ник> attachment [ключ]` | Данные модов (только чтение) |

Себя открыть нельзя. Подсказки ников берут онлайн-игроков и usercache, **без** чтения `playerdata/*.dat`.

В окне кнопка Read/Edit. Редактировать может только один модератор на цель; остальные видят «Занято» и имя в чате. Замок сам сбрасывается через 5 минут бездействия. Право на правку проверяется на сервере при каждом действии.

### Права

Регистрируются через NeoForge `PermissionGatherEvent.Nodes` (LuckPerms их видит и автодополняет). Дефолт — OP 2.

Старые узлы сохранены как запасные:

- `cointcore.invsee` — команда и запасной просмотр всех разделов, включая офлайн
- `cointcore.invsee.edit` — запасное редактирование всех разделов

Явный `false` на гранулярном узле в LuckPerms перекрывает запасной узел.

| Узел | Описание |
|------|----------|
| `cointcore.invsee.view.<inventory\|ender\|curios\|cosmetic\|backpack\|pocket\|moddata>` | Просмотр раздела |
| `cointcore.invsee.edit.<inventory\|ender\|curios\|cosmetic\|backpack\|pocket\|moddata>` | Правка раздела |
| `cointcore.invsee.offline` | Офлайн-игроки |
| `cointcore.invsee.exempt` | Защитить свой инвентарь от младших ролей |
| `cointcore.invsee.exempt.bypass` | Открывать защищённых игроков |
| `cointcore.invsee.weight` | Целый ранг (дефолт: уровень OP × 10) |

Иерархия защиты: при `exempt` у цели зритель проходит только если у него `exempt.bypass` **или** его вес **строго больше** веса цели. Вес берётся в таком порядке: LuckPerms meta `invsee-weight` → вес основной группы LuckPerms → узел `cointcore.invsee.weight` → уровень OP.

### Примеры LuckPerms

```
# Хелпер: смотреть онлайн-инвентарь, без правки и без офлайна
/lp group helper permission set cointcore.invsee true
/lp group helper permission set cointcore.invsee.offline false
/lp group helper permission set cointcore.invsee.view.backpack false

# Модератор: офлайн и правка инвентаря/эндера
/lp group moder permission set cointcore.invsee true
/lp group moder permission set cointcore.invsee.edit.inventory true
/lp group moder permission set cointcore.invsee.edit.ender true
/lp group moder meta set invsee-weight 20

# Админ: всё, защита своего инвентаря, обход чужой защиты
/lp group admin permission set cointcore.invsee true
/lp group admin permission set cointcore.invsee.edit true
/lp group admin permission set cointcore.invsee.exempt true
/lp group admin permission set cointcore.invsee.exempt.bypass true
/lp group admin meta set invsee-weight 50
```

### Журнал

Действия пишутся в `logs/cointcore-invsee/ГГГГ-ММ-ДД.log` (UTC): кто открыл чей инвентарь, режим, какие предметы сдвинуты (id, количество, слот). Без лишнего шума.

Офлайн-данные пишутся атомарно (temp + `.dat_old`) и **только если были правки**. В файл игрока попадают лишь инвентарные ключи: измерение и координаты не меняются.

## Права доступа

Права регистрируются через NeoForge Permission API. С LuckPerms используйте узлы вида `cointcore.<имя>`.

| Узел | По умолчанию | Описание |
|------|--------------|----------|
| `cointcore.vanish` | OP | Невидимость |
| `cointcore.vanish.mobs` | OP | Скрытие от мобов |
| `cointcore.vanish.see` | OP | Видеть невидимых |
| `cointcore.nv` | OP | Ночное зрение |
| `cointcore.fly` | OP | Полёт |
| `cointcore.god` | OP | Режим бога |
| `cointcore.msg` | все | Личные сообщения |
| `cointcore.ignore` | все | Игнор-лист |
| `cointcore.mute` | OP | Мут |
| `cointcore.unmute` | OP | Снятие мута |
| `cointcore.ban` | OP | Бан |
| `cointcore.tpl` | OP | Телепорт к офлайн-игроку |
| `cointcore.warn` | OP | Варны |
| `cointcore.punishments` | OP | История наказаний |
| `cointcore.turn_pvp` | все | Переключение PvP |
| `cointcore.keep_inventory` | запрещено | Keep Inventory при смерти |
| `cointcore.claim_flags` | OP | Управление флагами чанков |
| `cointcore.kit_credits` | OP | Управление кредитами на киты |
| `cointcore.vote.day` | все | Голосование за день |
| `cointcore.vote.clear_weather` | все | Голосование за погоду |
| `cointcore.restart` | OP | Рестарт сервера |
| `cointcore.reload` | OP | Перезагрузка конфигов |
| `cointcore.spy` | OP | Слежка за чатом |
| `cointcore.chunklimit` | OP | Управление лимитами чанков |
| `cointcore.chunklimit.bypass` | OP | Обход лимитов чанков |
| `cointcore.adminchat` | OP | Админ-чат |
| `cointcore.invsee` | OP | InvSee: базовый просмотр (см. [InvSee](#invsee)) |
| `cointcore.invsee.edit` | OP | InvSee: запасная правка всех разделов |
| `cointcore.watchdog` | OP | Tick watchdog: команда и уведомления о лагах |

## Tick watchdog

Постоянный серверный монитор лагов. Когда тик тормозит, в отчёте видны **какие** block entity / entity виноваты, **где** они стоят и **какие методы каких модов** едят время. Spark для этого не нужен.

### Что показывает

- Топ ticking block entity и entity: id типа, мод, измерение, координаты, чанк, суммарное/среднее время.
- Если установлен FTB Chunks: владелец/команда клейма и force-load (`ftbchunks`, `vanilla`, `ftbchunks+vanilla`, иначе `loaded` если чанк просто загружен).
- Топ методы по self-time на медленных тиках: «метод X мода Y занял N% времени медленных тиков», с привязкой к block entity, который тикался в момент семпла.
- Статистика окна: avg / p95 / max MSPT, оценка TPS, число медленных тиков.

Детальный nanoTime вокруг каждого BE/entity включается **только после недавнего медленного тика** (режим `auto`). На спокойном сервере это два `nanoTime` на тик плюс счётчики.

Оценка оверхеда: в простое **<0.01 ms/тик**; в детальном режиме обычно **0.05–0.3 ms/тик** (зависит от числа тикающихся BE/entity); семплирование стека идёт в отдельном daemon-потоке и не крутится, пока нет медленного тика.

### Команды

| Команда | Описание |
|---------|----------|
| `/cointcore watchdog top` | Текущий топ виновников в чат |
| `/cointcore watchdog report` | Сразу записать и показать отчёт (полный файл — на диске) |
| `/cointcore watchdog tp <n>` | Телепорт к записи №n из последнего топа |
| `/cointcore watchdog start` | Включить в рантайме |
| `/cointcore watchdog stop` | Выключить в рантайме |

Право: `cointcore.watchdog` (OP 2 по умолчанию, узел через `PermissionGatherEvent`). Онлайн-админы с этим правом получают предупреждение в чат при серии медленных тиков.

### Как читать отчёт

Файлы: `logs/cointcore-watchdog/ГГГГ-ММ-ДД.log` (UTC, дописывается в течение дня).

1. Блок статистики тиков — есть ли устойчивый лаг (p95/max vs порог).
2. Топ block entity — обычно главный виновник (пример: `enderio:item_conduit` у склада Functional Storage). Берите `pos` и `chunk`, смотрите `claim` / `forceload`.
3. Топ entity — мобы, contraption, item entities.
4. Топ методы — если BE сам по себе «дешёвый», а внутри него тяжёлый чужой мод (Relics copy, Curios scan и т.п.).

### Конфиг `config/cointcore/tick-watchdog.json`

| Ключ | Дефолт | Смысл |
|------|--------|--------|
| `enabled` | `true` | Главный выключатель |
| `slowTickThresholdMs` | `100` | Тик считается медленным |
| `sustainedSlowTicks` | `3` | Подряд медленных тиков для варнинга |
| `windowSeconds` | `60` | Окно агрегации / размер кольца тиков |
| `detailedTimingMode` | `auto` | `auto` / `always` / `off` — nanoTime вокруг BE/entity |
| `autoDetailedAfterSlowTicks` | `1` | В `auto` включать детализацию после N медленных |
| `samplingIntervalMs` | `1` | Интервал семплов стека Server thread |
| `samplingMaxDurationMs` | `80` | Сколько семплировать после обнаружения медленного тика |
| `topEntries` | `15` | Сколько строк в отчёте |
| `reportIntervalSeconds` | `300` | Как часто писать дневной файл |
| `maxReportAgeDays` | `7` | Retention отчётов (0 = без лимита по возрасту) |
| `maxReportTotalSizeMb` | `512` | Retention отчётов по суммарному размеру (0 = без лимита) |
| `notifyAdmins` | `true` | Писать топ-3 в чат админам |
| `notifyCooldownSeconds` | `60` | Антиспам варнингов |
| `attributeBlockEntities` | `true` | Замерять block entity |
| `attributeEntities` | `true` | Замерять entity |

Retention старых отчётов: хранятся **7 дней или 512 MB**, что наступит раньше.

Автопрофили Spark (`config/cointcore/spark-profiler.json`) тоже чистится: `maxProfileAgeDays` (7) и `maxProfileTotalSizeMb` (5120 ≈ 5 GB). Старые `.spark` в `spark/` удаляются, чтобы снова не набрать сотни гигабайт.

## Конфигурация

Конфиги создаются при первом запуске. Большинство перечитывается через `/cointcore reload`.

| Файл | Описание |
|------|----------|
| `config/cointcore/server-automation.json` | Плановый рестарт, периодические сообщения |
| `config/cointcore/world-cleanup.json` | Очистка предметов на земле |
| `config/cointcore/mob-cleanup.json` | Очистка мобов |
| `config/cointcore/votes.json` | Голосования (день, погода, сон) |
| `config/cointcore/chunk-limits.json` | Лимиты блоков и сущностей на чанк |
| `config/cointcore/join-messages.json` | Сообщения при входе/выходе |
| `config/cointcore/admin-chat.json` | Формат админ-чата |
| `config/cointcore/spark-profiler.json` | Автопрофилирование Spark + retention профилей |
| `config/cointcore/tick-watchdog.json` | Tick watchdog (координаты лагов, семплы методов) |
| `config/cointcore/me-unique-filter.json` | Уникальные фильтры ME (AE2) |
| `config/relpchatprefix/config.json` | Префиксы relay-чата |

Данные игроков (муты, киты, флаги чанков, история наказаний) хранятся в `world/data/` через Minecraft SavedData.

## Интеграции

Мод использует опциональные зависимости — при отсутствии мода соответствующая функция не активируется.

| Мод | Что даёт |
|-----|----------|
| **LuckPerms** | Расширенное управление правами |
| **FTB Chunks / Teams** | Защита чанков, флаги команд |
| **FTB Essentials** | Киты, офлайн-телепорт, фиксы NBT |
| **Curios / Accessories** | Keep Inventory, донорский полёт |
| **AE2** | Уникальные ME-фильтры, spatial storage guard |
| **Apothic Spawners** | Лимиты и лут спавнеров |
| **Spark** | Метрики TPS/MSPT, профилирование |
| **TAB** | Плейсхолдеры `%cointcore_tps%`, `%cointcore_mspt%` и др. |
| **Placeholder API** | Плейсхолдеры `%cointcore:tps%`, `%cointcore:mspt%` и др. |
| **Create, Quarry+, Ars Nouveau, ...** | Защита чанков через mixin |

## Структура проекта

```
src/main/java/com/mawlee/cointcore/
├── command/          # Brigadier-команды
├── config/           # JSON-конфиги (Gson)
├── claim/            # FTB Chunks: ClaimGuard, флаги
├── mixin/            # Mixin + CointCoreMixinPlugin (условная загрузка)
├── vanish/           # Невидимость
├── mute/, ban/, punishment/  # Модерация
├── watchdog/         # Tick watchdog: атрибуция, семплер, retention
├── server/           # Рестарты, очистка мира, авто-spark
├── chunklimit/       # Лимиты на чанк
├── spawner/          # Спавнеры
├── keepinventory/    # Keep Inventory
├── ae/               # Applied Energistics 2
├── ftb/              # FTB Chunks / Teams / Essentials
├── luckperms/        # LuckPerms (reflection)
├── placeholder/      # TAB и Placeholder API
└── ...
```

## Лицензия

All Rights Reserved — см. `gradle.properties`.

## Автор

mawlee
