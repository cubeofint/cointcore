# CointCore

Серверный coremod для модпака **COINT 1.2\*** на **Minecraft 1.21.1** / **NeoForge**.

Мод объединяет модерацию, чат, автоматизацию сервера, защиту чанков, экономику глюонов и интеграции с tech-модами в одном JAR. Ставится **на сервер и на клиенты одной и той же сборкой**: мод регистрирует блок `cointcore:trader` и клиентские экраны (торговый терминал, InvSee), а `displayTest = MATCH_VERSION` требует совпадения версий.

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
- [Глюоны и торговец](#глюоны-и-торговец)
- [Лаг-фиксы ATM10 8.2](#лаг-фиксы-atm10-82)
- [Интеграции](#интеграции)
  - [Мост FTB Ranks → LuckPerms](#мост-ftb-ranks--luckperms)
- [Структура проекта](#структура-проекта)

## Возможности

### Модерация и социальные функции

- Личные сообщения (`/msg`, `/r`) и игнор-лист
- Муты, баны, варны и история наказаний
- Невидимость для игроков и мобов (`/vanish`, `/v`)
- Слежка за чатом (`/spy`)
- Админ-чат (`/a`, `/ac`, `/adminchat`)
- Телепорт к офлайн-игроку (`/tpl`)
- Просмотр и правка инвентарей (`/invsee`) — единое окно с вкладками, онлайн и офлайн, с журналом действий

### Серверная автоматизация

- Плановый и ручной рестарт (`/cointcore restart`)
- Периодические сообщения в чат
- Очистка предметов на земле (`world-cleanup`)
- Защита от массового выпадения предметов: при ломании контейнера с огромным содержимым лишнее упаковывается в «кучи предметов» (`item-perf.json`)
- Очистка мобов по расписанию (`mob-cleanup`)
- Сброс выбранных измерений по расписанию с рестартом (`dimension-wipe.json`, `/cointcore dimwipe`)
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
- Лимиты блоков на чанк, команду и игрока; лимиты сущностей на чанк и команду (`/cointcore chunklimit`)
- Лимиты и перехват лута спавнеров (Apothic Spawners)
- Кредиты на киты FTB Essentials (`/kit balance`, `/cointcore kit ...`) и стартовый кит (`/cointcore starter`)
- Кошелёк глюонов: `/balance`, `/pay`, админ `/cointcore gluons`, системный терминал `cointcore:trader`, торговый автомат игрока `cointcore:player_trader`, обмен с сайтом через AzLink
- Уникальные фильтры ME (AE2)
- Донорские привилегии (полёт и др.)

## Установка

1. Скачайте или соберите `cointcore-1.0.0.jar`.
2. Положите **один и тот же** JAR в `mods/` сервера и в `mods/` клиентов. Клиент без мода или с другой версией в список серверов попадёт как несовместимый.
3. Запустите сервер — конфиги создадутся автоматически в `config/cointcore/`.
4. Настройте права через LuckPerms или OP-уровень (см. [Права доступа](#права-доступа)).

> **Версия и реестры.** Если меняется набор блоков, меню или сетевых пакетов, поднимайте `mod_version` в `gradle.properties`. Иначе клиент со старым JAR той же версии пройдёт проверку версии, но отвалится при синхронизации реестров.

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
| `/balance` / `/money` | Баланс глюонов |
| `/pay <игрок> <сумма>` / `/transfer` | Перевод глюонов (офлайн-цели через usercache, как mute) |

### Администрирование (`/cointcore`)

| Команда | Описание |
|---------|----------|
| `/cointcore reload` | Перезагрузить конфиги |
| `/cointcore restart [секунды]` | Запланировать рестарт (`cancel` — отменить) |
| `/cointcore worldcleanup status` | Статус очистки мира |
| `/cointcore worldcleanup run` | Запустить очистку вручную |
| `/cointcore dimwipe status\|now` | Сброс измерений: статус расписания / сбросить сейчас |
| `/cointcore sunkencity status\|force` | Респавн Sunken City |
| `/cointcore cataclysmspots status\|force` | Респавн структур Cataclysm (`frostedprison` — старый алиас) |
| `/cointcore starter status\|set_from_inv\|sync_cooldown\|reset_firstjoin` | Стартовый кит |
| `/cointcore chunklimit ...` | Управление лимитами чанков |
| `/cointcore claim flag ...` | Флаги чанков FTB (если FTB Chunks установлен) |
| `/cointcore kit balance\|add\|set\|take` | Управление кредитами на киты (если FTB Essentials установлен) |
| `/cointcore watchdog ...` | Tick watchdog: отчёт, топ, телепорт к виновнику |
| `/cointcore gluons get\|set\|add` | Админ-кошелёк глюонов (`cointcore.gluons.admin`) |

#### Флаги чанков (`/cointcore claim flag`)

| Подкоманда | Описание |
|------------|----------|
| `info <команда\|игрок>` | Показать флаги команды |
| `mob-spawn <цель> <allow\|deny>` | Разрешить/запретить спавн всех мобов |
| `mob-spawn <цель> <моб> <allow\|deny\|clear>` | Правило для конкретного моба (`clear` — снять) |
| `mob-damage <цель> <true\|false>` | Урон мобам в привате |
| `fire-spread <цель> <true\|false>` | Распространение огня |
| `pvp <цель> <true\|false>` | PvP в привате |
| `entry <цель> <allow\|deny>` | Вход чужих игроков в приват |

Каждый флаг проверяет свой узел `cointcore.claim_flag.*` (см. [Права доступа](#права-доступа)).

#### Лимиты блоков и сущностей (`/cointcore chunklimit`)

Три области блоковых лимитов проверяются вместе — постановка запрещается, если превышена любая:

| Область | Префикс команды | Секции JSON | Что считается |
|---------|-----------------|-------------|---------------|
| Чанк | — | `blockLimits`, `groups` | Блоки в чанке |
| Команда | `team` | `teamBlockLimits`, `teamGroups` | Блоки во всех приватах FTB-команды (общий пул участников) |
| Игрок | `player` | `playerBlockLimits`, `playerGroups` | Блоки, поставленные самим игроком, во всех измерениях |

Ключи: точный id (`minecraft:hopper`), тег (`tag c:chests` в команде, `#c:chests` в JSON), маска мода (`mod ae2` / `ae2:*`), группа. Каждый блок считается по одному, самому точному ключу. Лимиты сущностей (`entity ...`) есть для чанка и команды.

```
/cointcore chunklimit player block set minecraft:hopper 4
/cointcore chunklimit team block set minecraft:hopper 6
/cointcore chunklimit player check [игрок]
/cointcore chunklimit entity set alexsmobs:crow 5
```

Личный счётчик ведёт владелец блока (`<world>/data/cointcore_block_owners.dat`). Владелец снимается, когда блок ломают или меняют, а при загрузке чанка записи сверяются с миром. В личный лимит попадают блоки, поставленные после того, как лимит задан. Постановки машинами (FakePlayer) проверяются только лимитами чанка и команды.

## InvSee

Просмотр и правка чужих инвентарей в едином админ-окне с вкладками сверху:

- **Инвентарь** — основной инвентарь, хотбар, броня, вторая рука.
- **Эндер-сундук** — 27 слотов.
- **Accessories** — реальные слоты Curios цели (косметические — только у типов слотов с включённой косметикой), с иконками и подсказками. Без Curios вкладка показывает слоты мода Accessories; без обоих модов скрыта. Правки проверяются на сервере (`CuriosApi.isStackValid`).
- **FTB** — дома и последняя смерть FTB Essentials, только чтение.
- **Могилы** — последняя смерть и могилы YIGD / Tombstone / Gravestone, только чтение; без мода могил вкладка скрыта.
- **Состояние** — здоровье, еда, опыт, эффекты, позиция, режим игры, онлайн/офлайн.

ПКМ по шалкеру, мешку или рюкзаку открывает вложенное содержимое. Логика на сервере, клиент получает оформление вкладок через пакеты.

### Команды

| Команда | Раздел |
|---------|--------|
| `/invsee <ник\|uuid>` | Инвентарь, броня, вторая рука |
| `/invsee <ник> ender` | Эндер-сундук |
| `/invsee <ник> accessories` | Вкладка Accessories (Curios / Accessories) |
| `/invsee <ник> ftb` | Дома и последняя смерть FTB |
| `/invsee <ник> graves` | Могилы |
| `/invsee <ник> state` | Состояние игрока |
| `/invsee <ник> curios` | Curios (отдельное окно) |
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
| `cointcore.invsee.view.<inventory\|ender\|curios\|cosmetic\|backpack\|pocket\|moddata\|accessories\|ftb\|graves\|state>` | Просмотр раздела / вкладки |
| `cointcore.invsee.edit.<inventory\|ender\|curios\|cosmetic\|backpack\|pocket\|moddata\|accessories>` | Правка раздела |
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
| `cointcore.claim_flag.mob_spawn` | OP | Флаг `mob-spawn` у приватов FTB |
| `cointcore.claim_flag.mob_damage` | OP | Флаг `mob-damage` |
| `cointcore.claim_flag.fire_spread` | OP | Флаг `fire-spread` |
| `cointcore.claim_flag.pvp` | OP | Флаг `pvp` |
| `cointcore.claim_flag.entry` | OP | Флаг `entry` |
| `cointcore.claim_flag.entry.bypass` | OP | Входить в приваты, закрытые для чужих |
| `cointcore.claim.buffer.bypass` | OP | Захват чанков в буферной зоне между командами |
| `cointcore.claim.boss_arena.bypass` | OP | Приват чанков арены дракона Края и структур боссов Cataclysm |
| `cointcore.kit_credits` | OP | Управление кредитами на киты |
| `cointcore.starter` | все | `/kit start` — получить стартовый кит |
| `cointcore.starter.admin` | OP | Правка стартового кита и флагов первого входа |
| `cointcore.afk.bypass` | OP | Без предупреждений, метки `[AFK]` и кика |
| `cointcore.afk.alerts` | OP | Уведомления о подозрительной активности во время AFK |
| `cointcore.flux.admin` | OP | Flux Networks: правка любых сетей |
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
| `cointcore.gluons.admin` | OP | Админ-команды `/cointcore gluons` |
| `cointcore.gluons.balance` | все | `/balance` и `/money` |
| `cointcore.gluons.pay` | все | `/pay` и `/transfer` |
| `cointcore.playershop.place` | все | Поставить торговый автомат |
| `cointcore.playershop.use` | все | Открыть автомат и торговать |
| `cointcore.playershop.admin` | OP | Управление/ломка чужого автомата |

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
| `config/cointcore/chunk-limits.json` | Лимиты блоков (чанк / команда / игрок) и сущностей (чанк / команда) |
| `config/cointcore/claims.json` | Буфер между командами, бонус чанков, запрет привата арен боссов (`bossClaimGuard`) |
| `config/cointcore/join-messages.json` | Сообщения при входе/выходе |
| `config/cointcore/admin-chat.json` | Формат админ-чата |
| `config/cointcore/spark-profiler.json` | Автопрофилирование Spark + retention профилей |
| `config/cointcore/tick-watchdog.json` | Tick watchdog (координаты лагов, семплы методов) |
| `config/cointcore/trader_offers.json` | Офферы торговца, `commission_percent`, кольцо истории цен |
| `config/cointcore/currency-movement.json` | Обмен глюонами с сайтом через AzLink (`site_queue_enabled`, `site_movements_enabled`, оба выключены по умолчанию) и опциональный HTTP-хук |
| `config/cointcore/item-perf.json` | Оптимизация item entity и защита от массового выпадения (`massDropGuard` и лимиты) |
| `config/cointcore/dimension-wipe.json` | Расписание сброса измерений |
| `config/cointcore/starter-kit.json` | Стартовый кит |
| `config/cointcore/afk.json` | AFK: пометка и кик |
| `config/cointcore/ftbranks-luckperms-bridge.json` | Мост FTB Ranks → LuckPerms (`ftbranksLuckPermsBridge`, по умолчанию `false`) |
| `config/cointcore/me-unique-filter.json` | Уникальные фильтры ME (AE2) |
| `config/cointcore/storage-perf.json` | Кэш AE2 fuzzy search, RS importer idle-skip |
| `config/cointcore/tick-throttles.json` | Throttle машин (MGU Saw, RFTools Builder и др.) |
| `config/cointcore/lag-fixes.json` | Лаг-фиксы ATM10 8.2 (см. ниже) |
| `config/relpchatprefix/config.json` | Префиксы relay-чата |

Это основные файлы. Остальные конфиги (perf-настройки отдельных модов, респавн структур, чат-релей и т.д.) создаются там же, в `config/cointcore/`.

Данные игроков (муты, киты, флаги чанков, история наказаний) хранятся в `world/data/` через Minecraft SavedData.

## Глюоны и торговец

Кошелёк: overworld SavedData `cointcore_gluon_wallets`. `/pay` и `/transfer` атомарны, цель может быть офлайн (тот же resolve, что у mute). `/cointcore gluons` только для админов.

### Торговый терминал `cointcore:trader`

**Как поставить и открыть**

1. Получите блок: `/give @s cointcore:trader` или вкладка «Функциональные блоки» в творческом режиме. Рецепта крафта нет, терминалы ставит администрация.
2. Поставьте блок — лицевая сторона (клавиатура и слот для карты) повернётся к игроку. Ломается киркой, при разрушении выпадает сам блок.
3. ПКМ по терминалу открывает окно: баланс игрока, список офферов со страницами и прокруткой колесом, у каждого оффера цена покупки и продажи с иконкой глюона и график цены.
4. «Купить» / «Продать» — один лот. С Shift — столько лотов, сколько влезает в одну стопку, с учётом баланса, места в инвентаре и наличия предмета.

Клиент только отправляет запрос (`TraderTradePayload`). Все проверки — нехватка глюонов, полный инвентарь, нет предмета, оффер недоступен — выполняет сервер; ответ приходит в окно (`TraderFeedbackPayload`) и в чат.

**Офферы и комиссия** — `config/cointcore/trader_offers.json`, перечитывается `/cointcore reload`:

```json
{
  "commission_percent": 2.5,
  "price_history_capacity": 48,
  "price_history_sample_interval_ticks": 1200,
  "price_history_average_band_percent": 5.0,
  "offers": [
    { "id": "diamond", "item": "minecraft:diamond", "count": 1, "buy_price": 100, "sell_price": 80, "enabled": true },
    { "id": "iron_ingot", "item": "minecraft:iron_ingot", "count": 16, "buy_price": 24, "sell_price": 16, "enabled": true }
  ]
}
```

| Поле оффера | Смысл |
|-------------|-------|
| `id` | Ключ оффера (по нему же ведётся история цен); по умолчанию — id предмета |
| `item` | Id предмета; неизвестный id пропускается с предупреждением в логе |
| `count` | Размер лота, от 1 до максимального стака предмета |
| `buy_price` | Цена лота для покупателя в целых глюонах; `0` — оффер только на продажу |
| `sell_price` | Цена лота при продаже игроком; `0` — только на покупку |
| `enabled` | `false` скрывает оффер |
| `components` | Необязательно: data components предмета (формат `DataComponentPatch`) |

Комиссия считается от цены лота с округлением вверх (`Commission.of`): **покупатель** платит `buy_price + комиссия`, **продавец** получает `sell_price − комиссия`. По умолчанию `commission_percent` = 2.5. Цены в оффере фиксированные — меняются только правкой конфига.

Дополнительно для автоматов игроков (тот же файл):

| Ключ | Дефолт | Смысл |
|------|--------|-------|
| `player_shop_commission_percent` | как `commission_percent` | Комиссия автоматов игроков |
| `player_shop_crafting` | `true` | Рецепт крафта `cointcore:player_trader`; `false` отключает рецепт |

### Торговый автомат `cointcore:player_trader`

Игрок ставит свой автомат (крафт: железо, стекло, сундук, редстоун; или `/give` / творческий режим). Владелец пишется в BlockEntity при установке. Ломать и открывать вкладку «Управление» может только владелец или `cointcore.playershop.admin`. Взрывы блок почти не берут (высокая blast resistance). Воронки и трубы не подключаются: BlockEntity не Container и не выставляет item handler.

**Сделки и сайт.** Выручка не копится в блоке для последующего «забрать». На каждой сделке кошельки меняются сразу, как `/pay` (владелец может быть офлайн):

- Покупка у автомата (`player_shop_buy`): покупатель −`buy_total` (цена + комиссия), владелец +`buy_price`. Комиссия сжигается.
- Скупка автоматом (`player_shop_sell`): владелец −`sell_price`, продавец +`sell_net` (цена − комиссия). Нет глюонов у владельца или места на складе — отказ.

Поле «продажи» в управлении — только статистика (`lifetime_revenue`), не баланс блока. Движения уходят в outbox с `deltas` по обоим игрокам.

Склад 27 слотов, офферы (предмет из ghost/склада, количество лота, цена продажи игроку и/или скупки у игрока, целое ≥ 1, одну сторону можно выключить нулём). Покупатель видит то же окно, что у системного терминала (без спарклайна), имя владельца в заголовке, «нет в наличии». Все проверки на сервере.

### История цен

История цен покупки пишется в overworld SavedData `cointcore_trader_price_history`: семпл на каждую сделку и периодически (`price_history_sample_interval_ticks`, по умолчанию 1200 = 60 с). Кольцо до `price_history_capacity` точек (48). Клиент получает историю вместе с офферами и рисует спарклайн 40×14. Цвет относительно среднего: зелёный — дешевле, красный — дороже, серый — в пределах `price_history_average_band_percent` (±5% по умолчанию). Подсказка по наведению: мин / среднее / макс / сейчас и вердикт.

Иконка глюона: `src/main/resources/assets/cointcore/textures/gui/gluon.png` (16×16, в GUI рисуется 8×8) — та же иконка, что на сайте. Чтобы заменить, положите новый файл **ровно по этому пути** и пересоберите JAR. В интерфейсе иконка стоит рядом с балансом и рядом с ценами оффера вместо слова «глюонов».

### Обмен с сайтом через AzLink

Движения валюты пишутся в overworld SavedData `cointcore_currency_movements` (`pay`, `trader_buy`, `trader_sell`, `admin_set`, `admin_add`). Это outbox для раздела сайта «движение валют». Переводы сайт↔сервер (`site_to_server` / `server_to_site` / `site_adjust`) тоже пишутся локально, но на сайт не отправляются. В JSON уходят `deltas` (uuid / signed delta / `balance_after` по каждому игроку) и `site_op_id` (id операции сайта или `null`).

**AzLink / сайт:** у cointcore нет compile-зависимости на AzLink. Обмен идёт рефлексией через `com.azuriom.azlink.common.coins.CoinOperationsBridge` — он есть в `main` репозитория [cubeofint/AzLink-mods](https://github.com/cubeofint/AzLink-mods) (PR #2–#5). На сервере должна стоять сборка AzLink из этой ветки: в релизе `AzLink-NeoForge-1.3.11` моста нет, и обмен с сайтом работать не будет. Токен сайта хранится только в конфиге AzLink, cointcore его не знает.

Настройки в `config/cointcore/currency-movement.json` (всё выключено по умолчанию):

| Ключ | Дефолт | Смысл |
|------|--------|-------|
| `site_queue_enabled` | `false` | Опрашивать очередь операций сайта и применять их к кошельку |
| `site_queue_poll_seconds` | `5` | Интервал опроса очереди (1–300 с) |
| `site_movements_enabled` | `false` | Отправлять движения кошелька на сайт (`postMovements`) |
| `site_movements_batch` | `100` | Размер пакета движений (1–200) |
| `enabled` + `endpoint_url` | `false` | Отдельный HTTP POST-хук для внешнего приёмника |

Отправка движений: пакеты, идемпотентность по id движения, retry с backoff 5 с…5 мин, курсор `site_sent_up_to` пишется только после `accepted_up_to` от сайта. Это журнал серверного кошелька: баланс сайта не меняется абсолютной перезаписью. HTTP-хук тоже не должен вызывать API абсолютного баланса.

### Авторитет сайта и очередь adjust

Авторитетный баланс — **per-server баланс на сайте** (`user_server_balances` / mc-azlink). Игровой кошелёк cointcore сходится к нему через очередь `GET /api/azlink/coins/operations`. Игровые изменения (`/pay`, торговец, `/cointcore gluons`) идут на сайт как movements с `deltas`. Обе стороны меняют сумму только операциями и дельтами, без абсолютного overwrite.

Направление `adjust`: signed `amount` (>0 кредит игрового кошелька, <0 дебет, `0` только `source=reconcile` — зонд без изменения, ack с `balance_after`). `adjust` не трогает глобальный кошелёк сайта. `to_server` / `from_server` без изменений. Ack: `applied` | `failed` плюс `balance_after`; повтор той же `id` отвечает тем же статусом (локальный ledger). Недостаточно средств на дебете — `insufficient_server_balance`, кошелёк не меняется.

Дрейф: если игра сообщила `balance_after`, нет pending ops, и баланс сайта ≠ `balance_after`, сайт ставит один corrective `adjust` (`source=reconcile`, amount = сайт − игра). Нулевой probe с тем же source позволяет узнать игровой баланс.

## Лаг-фиксы ATM10 8.2

Файл: `config/cointcore/lag-fixes.json`. **Все опции по умолчанию выключены** — просто положить JAR на прод ничего не меняет, пока админ не включит нужные секции.

### Рекомендуемый конфиг для нового сервера ATM10 8.2

```json
{
  "jdt_portals": {
    "enabled": true,
    "mode": "none",
    "clear_stale_tickets_on_start": true
  },
  "enderio_item_conduits": {
    "enabled": true,
    "tick_interval": 10,
    "max_slots_per_pass": 64,
    "idle_backoff_ticks": 40,
    "skip_unchanged_inventories": true
  },
  "relics_backpack_scan": {
    "enabled": true,
    "mode": "skip_nested",
    "scan_interval_ticks": 20
  },
  "chunk_loaders": {
    "enabled": true,
    "disable_ae2_spatial_anchor": true,
    "disable_compact_machines_chunkloader": true,
    "disable_hnn_data_center": true,
    "disable_ie_chunk_loader": true,
    "disable_railcraft_world_spike": true,
    "disable_steves_carts_chunk_loader": true
  }
}
```

### Опции

| Секция / ключ | Default | Описание |
|---|---|---|
| `jdt_portals.enabled` | `false` | Чинить force-load порталов Just Dire Things (Portal Gun / V2) |
| `jdt_portals.mode` | `"none"` | `none` — не грузить чанки; `owner_online` — только пока владелец онлайн |
| `jdt_portals.clear_stale_tickets_on_start` | `true` | При старте снять старые tickets `justdirethings:chunk_loader` |
| `enderio_item_conduits.enabled` | `false` | Soft-throttle item conduit сетей |
| `enderio_item_conduits.tick_interval` | `10` | Мин. интервал между проходами при активном переносе |
| `enderio_item_conduits.max_slots_per_pass` | `64` | Лимит слотов extract-инвентаря за проход |
| `enderio_item_conduits.idle_backoff_ticks` | `40` | Пауза после прохода, который ничего не перенёс |
| `enderio_item_conduits.skip_unchanged_inventories` | `true` | Пропускать повторный скан неизменённой сети после idle |
| `relics_backpack_scan.enabled` | `false` | Не сканировать содержимое Sophisticated Backpacks каждым тиком |
| `relics_backpack_scan.mode` | `"skip_nested"` | `skip_nested` или `throttle` |
| `relics_backpack_scan.scan_interval_ticks` | `20` | Интервал для `throttle` |
| `chunk_loaders.enabled` | `false` | Мастер-флаг ограничений chunk loader'ов |
| `chunk_loaders.disable_ae2_spatial_anchor` | `false` | AE2 Spatial Anchor |
| `chunk_loaders.disable_compact_machines_chunkloader` | `false` | Compact Machines chunkloader upgrade |
| `chunk_loaders.disable_hnn_data_center` | `false` | Hostile Neural Networks Data Center shell load |
| `chunk_loaders.disable_ie_chunk_loader` | `false` | Immersive Engineering Resonanz Observer |
| `chunk_loaders.disable_railcraft_world_spike` | `false` | Railcraft World Spike (+ minecart) |
| `chunk_loaders.disable_steves_carts_chunk_loader` | `false` | Steve's Carts chunk loader module |

### Рекомендации без автоматики (KubeJS / конфиги модов)

**Ars Additions** — в ATM10 8.2 `max_rituals` по умолчанию `Integer.MAX_VALUE`. Ограничьте вручную в конфиге Ars Additions:

```toml
# config/ars_additions-server.toml (имена секций могут отличаться)
# chunkloading -> max_rituals
max_rituals = 2
require_online = true
```

Если потребуется **убрать** крафт chunk loader'ов через KubeJS (альтернатива mixin, когда mixin нежелателен):

```js
// kubejs/server_scripts/coint_chunkloaders.js
ServerEvents.recipes(event => {
  event.remove({ output: 'ae2:spatial_anchor' })
  event.remove({ output: 'immersiveengineering:chunk_loader' })
  event.remove({ output: 'railcraft:world_spike' })
  event.remove({ output: 'railcraft:personal_world_spike' })
  event.remove({ output: 'railcraft:world_spike_minecart' })
  event.remove({ output: 'stevescarts:module_chunk_loader' })
  // Compact Machines chunkloader upgrade — id зависит от datapack/item registry:
  // event.remove({ mod: 'compactmachines', id: /chunk.?loader/ })
})
```

### Что не трогали

Секция `machine` в `tick-throttles.json` (MGU Saw / RFTools Builder budgets) **сохранена** — это отдельный generic machine throttle. AE2 AdvancedAE / ae2wtlib magnet throttle удалён как неэффективный по spark-профилям.

## Интеграции

### Обязательные моды на сервере

Без них CointCore не загрузится; NeoForge сообщит о недостающих зависимостях при старте.

| Мод | Что даёт |
|-----|----------|
| **FTB Library** | Общая база FTB-модов |
| **FTB Teams** | Команды, флаги команд, командные лимиты |
| **FTB Chunks** | Защита чанков, claim-лимиты |
| **FTB Ranks** | Мост к LuckPerms (см. ниже) |

### Опциональные интеграции

При отсутствии мода соответствующая функция не активируется.

| Мод | Что даёт |
|-----|----------|
| **LuckPerms** | Расширенное управление правами |
| **FTB Essentials** | Киты, офлайн-телепорт, фиксы NBT |
| **Curios / Accessories** | Keep Inventory, донорский полёт, вкладка Accessories в InvSee |
| **AzLink** (сборка из `cubeofint/AzLink-mods`) | Обмен глюонами с сайтом (см. [Глюоны и торговец](#глюоны-и-торговец)) |
| **YIGD / Tombstone / Gravestone** | Вкладка «Могилы» в InvSee |
| **AE2** | Уникальные ME-фильтры, spatial storage guard |
| **Apothic Spawners** | Лимиты и лут спавнеров |
| **Spark** | Метрики TPS/MSPT, профилирование |
| **TAB** | Плейсхолдеры `%cointcore_tps%`, `%cointcore_mspt%` и др. |
| **Placeholder API** | Плейсхолдеры `%cointcore:tps%`, `%cointcore:mspt%` и др. |
| **Create, Quarry+, Ars Nouveau, ...** | Защита чанков через mixin |

### Мост FTB Ranks → LuckPerms

Когда установлены и FTB Ranks, и LuckPerms, FTB Ranks перехватывает проверки команд (`command.*`) и отдаёт meta для FTB Essentials / Ultimine из своих рангов. Если в рангах узла нет, по умолчанию используется vanilla OP / дефолт конфига мода — **без запроса к LuckPerms**. Из‑за этого группы LuckPerms с `command.*` и meta вроде `ftbessentials.home.max` / `ftbultimine.max_blocks` не работают, пока FTB Ranks установлен.

CointCore добавляет mixin-мост (включается в конфиге, работает при установленном LuckPerms): если FTB Ranks не нашёл явное значение узла у игрока, запрос уходит в LuckPerms (permission node или meta). Явные значения в рангах FTB Ranks остаются приоритетнее. Если LuckPerms тоже молчит, сохраняется исходный fallback FTB Ranks.

**Как включить**

1. Откройте `config/cointcore/ftbranks-luckperms-bridge.json`.
2. Установите `"ftbranksLuckPermsBridge": true`.
3. По желанию включите `"ftbranksLuckPermsBridgeDebug": true` — в лог пишутся узел, игрок и источник (`FTB Ranks` / `LuckPerms` / `fallback`).
4. Выполните `/cointcore reload` или перезапустите сервер.

По умолчанию мост **выключен** (`false`): поведение байт-в-байт как без него.

> **Предупреждение:** после включения начинают действовать **все** уже прописанные в группах LuckPerms узлы `command.*` и meta FTB. Сначала проверьте группы helper / moderator / donor: лишние права (kick, tp, fly, god, kits и т.д.) сразу станут доступны игрокам этих групп.

**Чеклист ручной проверки на тестовом сервере (аккаунт без OP)**

1. Игрок в donor-группе с `command.fly` — `/fly` должен работать после включения моста.
2. Meta `ftbessentials.home.max` — `/sethome` учитывает лимит из LuckPerms (не только дефолт конфига).
3. Meta `ftbultimine.max_blocks` — лимит Ultimine берётся из LuckPerms.
4. Смена группы LuckPerms у онлайн-игрока — лимиты FTB Chunks (claim / force-load) обновляются без релога (мост дергает тот же refresh, что и chunk-bonus).
5. Выключите `ftbranksLuckPermsBridge` и перезагрузите конфиг — поведение снова как до моста.

## Структура проекта

```
src/main/java/com/mawlee/cointcore/
├── command/          # Brigadier-команды
├── config/           # JSON-конфиги (Gson)
├── permission/       # Узлы NeoForge Permission API
├── claim/            # FTB Chunks: ClaimGuard, флаги, буфер, запрет привата арен боссов
├── mixin/            # Mixin + CointCoreMixinPlugin (условная загрузка)
├── shop/             # Глюоны: кошелёк, торговый терминал, обмен с сайтом через AzLink
│   └── client/       #   экран терминала, спарклайн, иконка глюона
├── invsee/           # InvSee: меню, вкладки, интеграции, журнал
│   └── client/       #   экраны и оформление вкладок
├── item/             # Защита от массового выпадения, кучи предметов
├── vanish/           # Невидимость
├── mute/, ban/, punishment/  # Модерация
├── afk/              # AFK: пометка, кик, клиентская активность в GUI
├── watchdog/         # Tick watchdog: атрибуция, семплер, retention
├── server/           # Рестарты, очистка мира, сброс измерений, авто-spark
├── chunklimit/       # Лимиты на чанк
├── spawner/          # Спавнеры
├── keepinventory/    # Keep Inventory
├── kit/              # Кредиты на киты, стартовый кит
├── ae/               # Applied Energistics 2
├── ftb/, ftbessentials/, ftbranks/  # FTB Chunks / Teams / Essentials / Ranks
├── luckperms/        # LuckPerms (reflection)
├── placeholder/      # TAB и Placeholder API
└── ...               # интеграции с отдельными модами (adastra, ars, cataclysm, enderio, relics, ...)

src/main/resources/
├── assets/cointcore/ # lang, модель/текстура терминала, иконка глюона
├── data/             # лут терминала, теги блоков
└── cointcore.mixins.json, cointcore.compat.mixins.json
```

Тесты — JUnit 5 в `src/test/java`, проверяют чистую логику (комиссия, сделки, outbox, вкладки InvSee, watchdog и т.д.): `./gradlew test`.

## Лицензия

All Rights Reserved — см. `gradle.properties`.

## Автор

mawlee
