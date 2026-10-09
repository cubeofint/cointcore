# Глюоны и торговый терминал (состояние на main 7392e40)

## Решения пользователя
Ванильное окно (контейнер), торговля предмет ↔ глюоны, целые цены с округлением вверх, настраиваемая комиссия, права только через LuckPerms. Динамические рыночные цены без минимумов — заявлены как решённые, но В КОДЕ НЕ РЕАЛИЗОВАНЫ (цены фиксированные из конфига; есть только история цен).

## Блок
- `cointcore:trader` («Торговый терминал» / «Trade Terminal»): `shop/ShopBlocks` (DeferredRegister Blocks/Items, вкладка FUNCTIONAL_BLOCKS), `shop/TraderBlock` (HorizontalDirectionalBlock, FACING, VoxelShape по сторонам, прочность 3.5, кирка).
- Ресурсы: модель из `traderV3/trader.json` → `assets/cointcore/models/block/trader.json` (текстура `cointcore:block/trader`, 256×256), blockstate north/east90/south180/west270, лут, тег `mineable/pickaxe`.
- Иконка глюона в GUI: `assets/cointcore/textures/gui/gluon.png` (16×16, рисуется 8×8), взята с сайта (PR #23).

## Кошелёк и сделки
- `GluonWallet` + overworld SavedData `cointcore_gluon_wallets`. `/balance`, `/pay`, `/transfer` (атомарно, цель может быть офлайн), админ `/cointcore gluons`.
- Офферы: `config/cointcore/trader_offers.json` (`commission_percent`, история цен). `TraderOffer(id, display, count, buyPrice, sellPrice, buyFee, ...)`.
- Комиссия: `Commission.of`, округление вверх. Покупатель платит цена+комиссия, продавец получает цена−комиссия. `TraderDealMath`.
- GUI: `TraderMenu` (ванильный контейнер) + клиентский `shop/client/TraderScreen` (список офферов, купить/продать, Shift — стопка, две строки цен, спарклайн 40×14). Сделки только на сервере через payload `TraderTradePayload`; ответ `TraderFeedbackPayload`.
- История цен: SavedData `cointcore_trader_price_history`, семпл на сделку и раз в `price_history_sample_interval_ticks` (1200), кольцо `price_history_capacity` (48), полоса ±`price_history_average_band_percent` (5%).

## Сайт / AzLink
- Авторитет — per-server баланс на сайте (Azuriom `web/main-site`, `CurrencyLedger`, `user_server_balances`). Игровой кошелёк сходится к нему операциями/дельтами, без абсолютной перезаписи.
- Входящие операции: `SiteOperationPoller` → `GET /api/azlink/coins/operations` (to_server / from_server / adjust с signed amount, reconcile-зонд amount=0), применяет `SiteOperationApply`, ack applied|failed + `balance_after`, локальный ledger `SiteOperationSavedData` для идемпотентности.
- Исходящие: outbox `CurrencyMovementOutbox` (SavedData `cointcore_currency_movements`; типы pay, trader_buy, trader_sell, admin_set, admin_add, site_to_server, server_to_site, site_adjust) → `SiteMovementSender` батчами с `deltas` и `site_op_id`, retry 5 с…5 мин, курсор `site_sent_up_to` после `accepted_up_to`.
- Конфиг `config/cointcore/currency-movement.json`: `site_movements_enabled` = false по умолчанию; отдельный опциональный HTTP hook (`enabled` + `endpoint_url`).
- Без compile-зависимости: рефлексия в `com.azuriom.azlink.common.coins.CoinOperationsBridge` (есть в AzLink-mods origin/main после PR #2–#5 репозитория cubeofint/AzLink-mods).
- AzLink HttpClient: публичный `request(RequestMethod, endpoint, params, Class)`; `editCoins` 4-арг (с idempotency_key) в новых сборках, 3-арг в старых. Сайт: `/api/azlink/user/{id}/coins/{add|remove|withdraw|deposit}`, все требуют `idempotency_key`; add = TYPE_SERVER_EARN (дневной кап `daily_server_earn_cap`, по умолчанию 10000).

## Тесты (src/test/.../shop, 15 файлов)
Commission, TraderDealMath, TraderCommissionSides, TraderFeedbackKind, GluonTransfer, CurrencyMovement*(Type/Outbox/Nbt), SiteMovement(Sender/Payload), SiteOperation(+Apply), OfferBuyPriceHistory, OfferPriceVerdict, PriceSparklineLayout; config/CurrencyMovementConfigTest.

## Открытые пункты
- Динамическое ценообразование (спрос/предложение, без минимумов) не сделано.
- Деплой AzLink с `CoinOperationsBridge` (см. `mem:project/known_issues`).