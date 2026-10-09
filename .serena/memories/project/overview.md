# CointCore — обзор проекта (актуально на 2026-10-08, main = 7392e40, PR #23)

## Что это
Coremod для модпака COINT 1.2* (основа ATM10 8.2), Minecraft 1.21.1, NeoForge 21.1.228 (dev) / 21.1.251 (прод), Java 21.
mod_id `cointcore`, пакет `com.mawlee.cointcore`, версия 1.0.0. Репозиторий: github.com/cubeofint/cointcore.
Локальная папка: `F:\Lumen\mods src for mixins\CointCore_1.2x` (Windows, PowerShell).

## Сборка
- `./gradlew build` (локально: `.\gradlew.bat --gradle-user-home "$env:USERPROFILE\.gradle" build`, `--offline` работает, если кэш прогрет).
- Jar: `build/libs/cointcore-1.0.0.jar` (~1.6 МБ на 2026-10-08).
- С PR #6 сторонние моды под mixin берутся из Maven (Curse Maven, FTB, Illusive, Wisp, Modrinth, Central) — конфигурация `packMods` (compileOnly). ~50 координат `curse_*` в `gradle.properties`. Папка `server-mods` для сборки НЕ нужна (только для javap/декомпиляции).
- Деобф. классы MC для javap: `build\moddev\artifacts\neoforge-21.1.228.jar`.
- Тесты: JUnit 5 в `src/test` (39 сьютов, 147 тестов, все зелёные на 2026-10-08). Тестируется чистая логика без бутстрапа MC (политики, математика, layout, NBT-кодеки через простые структуры).
- CI: `.github/workflows/build.yml` — ubuntu, temurin 21, `./gradlew build --no-daemon` на push/PR в main.

## Метаданные мода
- `src/main/templates/META-INF/neoforge.mods.toml`: `displayTest="MATCH_VERSION"` (клиент обязан иметь ту же версию: есть блок `cointcore:trader`, меню, клиентские экраны). ~30 опциональных зависимостей `side="SERVER"`, одна `BOTH`.
- Mixin-конфиги: `cointcore.mixins.json` (server 33, client 4) и `cointcore.compat.mixins.json` (server 132, client 8, mixins 1) — сторонние моды в compat; оба через `CointCoreMixinPlugin` (условная загрузка по наличию мода).

## Структура (src/main/java/com/mawlee/cointcore, кол-во java-файлов)
mixin 175, invsee 80, config 53, shop 39, command 26, claim 15, watchdog 13, spawner 13, vanish 11, keepinventory 10, server 10, chunklimit 9, afk 8, privilege 7, punishment 6, mute 6, chatspy 6, item 6, ... Корень: CointCore, CointCoreEvents, CointCoreRuntimeCleanup, NightVisionEvents.
- Регистрации в конструкторе `CointCore` через `DeferredRegister` (`ShopBlocks.register`, `ShopMenus`, `InvSeeMenus.REGISTER` и т.д.).
- Сеть: custom payloads (`registrar.playToServer/playToClient`) в AfkNetwork, ClaimFlagEditNetwork, InvSeeNetwork, SeeInvisibleNetwork, TraderNetwork.
- Клиентский код (Dist.CLIENT): afk/client, invsee/client, shop/client, seeinvisible.
- Конфиги: JSON (Gson) в `config/cointcore/*.json`, перечитываются `/cointcore reload`; дефолты частично в `src/main/resources/cointcore/default_configs/`.
- Данные: overworld SavedData (`cointcore_gluon_wallets`, `cointcore_currency_movements`, `cointcore_trader_price_history`, и др.).
- Сообщения: `CointCoreMessages` + `assets/cointcore/lang/{ru_ru,en_us}.json`, `&`-коды через `LegacyTextParser`.
- Права: NeoForge PermissionAPI (`CointPermissionNodes`/`PermissionRegistration`), бэкенд LuckPerms.

## Документация
README.md (~41 КБ, рус.) — актуальный справочник: команды, права, конфиги, watchdog, глюоны/торговец, лаг-фиксы, интеграции, мост FTB Ranks→LuckPerms.

См. также `mem:project/workflow_rules`, `mem:features/gluon_trader`, `mem:features/mass_drop_item_piles`, `mem:project/known_issues`, `mem:project/history`.