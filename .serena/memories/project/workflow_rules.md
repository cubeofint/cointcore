# Правила работы с пользователем (CointCore)

## Общение
- Отвечать всегда на русском.
- Утвердительные формулировки, без противопоставлений «X, а не Y».
- После вопроса-анализа ничего не внедрять, пока не попросят.

## Git / PR
- Не коммитить, пока не попросят.
- Автор коммитов: только `mawleesys <alsoalberdo@gmail.com>` (локальный git config: user.name=mawlee — передавать автора явно: `git -c user.name=mawleesys -c user.email=alsoalberdo@gmail.com commit ...`).
- Без Cursor attribution, без `Co-authored-by`, PR без упоминаний Cursor.
- Ветки `feature/...` (не `cursor/...`). Один PR в main, squash-ready.
- Не следить и не чинить CI в цикле.
- Не коммитить секреты/токены сайта.
- В git НЕ попадают: `client-mods/`, `server-mods/`, `spark-profilers/`, `*.log`, `crash-*.txt` (в .gitignore), а также локальные `.cursor/`, `.serena/`, `.specify/`, `docs/`, `traderV3/` (неотслеживаемые, пользователь решил не коммитить).
- `.gitignore`: `/assets/` и `/com/`, `/META-INF/` только в корне; `src/main/resources/assets` отслеживается.

## Сборка/деплой
- После изменений кода CointCore пересобирать `build/libs/cointcore-1.0.0.jar`.
- compileOnly-jar сторонних модов не попадают в итоговый мод.
- `client-mods` / `server-mods` — рабочие наборы модов пользователя, не мусор, не удалять. Клиентам нужен тот же jar CointCore (MATCH_VERSION).

## Запреты (из прошлых задач)
- Не трогать: InvSee (если задача не про него), FTB Ranks bridge, lag-fixes, watchdog — когда задача про трейдер.
- Не трогать LuckPerms (права/группы только через LP, не часть трейдера).
- Не оптимизировать broom `move()` (EvilCraft); не троттлить клиентский Placed Item ticker (Draconic).
- Не менять механики Ad Astra, значения энергии/герметизации; не использовать `disableOxygen/Gravity/Temperature/AirVortexes`; не менять `planetRandomTickSpeed`.
- Не патчить Integrated Dynamics / Cataclysm / EvilCraft / Apotheosis / BWG / Oritech / Ice and Fire / Starbunclemania / Create ради каскадных NPE.
- Не форкать jar'ы FTB.
- Не включать платные модели.