# Известные несоответствия и риски (на 2026-10-08)

1. AzLink не готов к глюонам на проде: развёрнутые `server-mods/` и `client-mods/AzLink-NeoForge-1.3.11-1.21.1.jar` не содержат `com.azuriom.azlink.common.coins.CoinOperationsBridge`. Серверный jar к тому же старый (07/31): `editCoins` только 3-арг без idempotency_key. Локальный клон `web/AzLink-mods-1.21.1` на `main` b9ec598 отстаёт от origin/main (1df4e20, PR #2–#5 с мостом). Нужно: pull, собрать AzLink, задеплоить; применить site patch'и в `web/main-site`. Пока моста нет — поллер/отправка движений не работают (по умолчанию выключены).
2. README обновлён 2026-10-08 (установка на сервер+клиент, InvSee-вкладки, команды/права claim_flag.*, терминал и формат trader_offers.json, настройки currency-movement, мост AzLink в main, структура). Влит в main через PR #24 (6ee0cbd). Осталось: `mod_description` в gradle.properties всё ещё говорит «Install only on the dedicated server».
3. Динамические цены торговца не реализованы (см. `mem:features/gluon_trader`).
4. Версия мода не поднималась (1.0.0) при добавлении блока/реестров — клиент со старым jar 1.0.0 пройдёт MATCH_VERSION, но упадёт на синхронизации реестров. Рекомендовано поднимать `mod_version` при изменении реестров/пакетов.
5. (решено 2026-10-08) Устаревшие локальные ветки удалены; локально и на GitHub только `main`.
6. Пустая папка `src/main/resources/data/justdirethings/tags/block` (только локально, в jar даёт пустые записи каталогов) — безвредно.
7. `.specify/memory/constitution.md` — незаполненный шаблон speckit.