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

### Шаги

```bash
git clone https://github.com/cubeofint/cointcore.git
cd cointcore
./gradlew build
```

Готовый JAR: `build/libs/cointcore-1.0.0.jar`.

### Папка `modlist/`

Для компиляции mixin-классов нужны JAR сторонних модов в локальной папке `modlist/` (она в `.gitignore` и не входит в репозиторий). Скопируйте из вашего модпака COINT следующие файлы:

| JAR | Назначение |
|-----|------------|
| `create-1.21.1-6.0.10.jar` | Create |
| `appliedenergistics2-19.2.17.jar` | AE2 |
| `AdditionalEnchantedMiner-1.21.1-neoforge-21.1.153.jar` | QuarryPlus |
| `ars_nouveau-1.21.1-5.11.5.jar` | Ars Nouveau |
| `cc-tweaked-1.21.1-forge-1.113.1.jar` | ComputerCraft |
| `industrialforegoing-1.21-3.6.38.jar` | Industrial Foregoing |
| `Draconic-Evolution-1.21.1-3.1.4.632.jar` | Draconic Evolution |
| `BrandonsCore-1.21.1-3.2.1.309.jar` | Brandon's Core |
| `actuallyadditions-1.3.25+mc1.21.1.jar` | Actually Additions |
| `entangled-1.3.21-neoforge-mc1.21.jar` | Entangled |
| `ApothicSpawners-1.21.1-1.3.4.jar` | Apothic Spawners |
| `cyclopscore-1.21.1-neoforge-1.29.1.jar` | Cyclops Core |
| `supermartijn642corelib-1.1.21-neoforge-mc1.21.jar` | CoreLib |

Версии должны совпадать с указанными в `build.gradle`. FTB-моды и LuckPerms подтягиваются из Maven автоматически.

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
| `config/cointcore/spark-profiler.json` | Автопрофилирование Spark |
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
├── server/           # Рестарты, очистка мира
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
