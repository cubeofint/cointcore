# История PR (github.com/cubeofint/cointcore), все MERGED

- #1–#3 (2026-07-02): gradlew executable, чистка артефактов, README.
- #4 (10-06): блок торгового терминала + накопленные серверные фичи (кучи предметов, mixins, конфиги); `.gitignore` чинит `assets/`.
- #5: InvSee — надёжность, права, журнал (этап 1).
- #6: сборка без `server-mods` (Maven ATM10 8.2).
- #7: мост FTB Ranks → LuckPerms (выкл. по умолчанию).
- #8: Tick watchdog (координаты лагов, retention Spark-профилей).
- #9: лаг-фиксы ATM10 8.2 (порталы JDT, EnderIO conduits, Relics backpacks, chunk loaders) — всё выкл. по умолчанию, `lag-fixes.json`.
- #10: фикс краша Relics backpack mixin, compat-конфиг mixin'ов.
- #11: фикс PlayerListMixin InvSee (Optional в 1.21.1).
- #12: Trader фаза 1 — ванильное меню по использованию.
- #13: фаза 2 — кошелёк глюонов, комиссия, баланс в терминале.
- #14: фаза 3 — команды глюонов, офферы, outbox движений.
- #15: приём переводов с сайта через AzLink в кошелёк.
- #16: доставка движений на сайт через AzLink (outbox).
- #17: GUI торговца — офферы, купить/продать, пакеты, спарклайн.
- #18: InvSee этап 2 — вкладки админ-GUI.
- #19: фикс CME агрегатора watchdog, halt guard при остановке.
- #20: site adjust операции и movement deltas.
- #21: InvSee — слоты Curios на вкладке Accessories; две строки цен в терминале.
- #22: InvSee Accessories — иконки и тултипы Curios.
- #23 (10-08): иконка глюона из файла сайта.
- #24 (10-08): README приведён к текущему коду (squash 6ee0cbd, автор mawleesys, без трейлеров). Мерж делается `gh pr merge --squash --subject ... --body ...` с явным телом, чтобы GitHub не добавил соавторов; main без branch protection.

Связанный репозиторий cubeofint/AzLink-mods (origin/main 1df4e20): #2 мост очереди операций, #3 movements, #4 server adjust + ackWithBalance, #5 site patch 0004.