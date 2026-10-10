# Глюоны, системный терминал и автомат игрока

## Решения пользователя
Ванильное окно (контейнер), торговля предмет ↔ глюоны, целые цены с округлением вверх, настраиваемая комиссия, права только через LuckPerms. Динамические рыночные цены без минимумов — заявлены как решённые, но В КОДЕ НЕ РЕАЛИЗОВАНЫ (цены системного терминала фиксированные из конфига; есть только история цен).

Системный терминал `cointcore:trader` не менять поведением. Автомат игрока — отдельный блок `cointcore:player_trader`.

## Системный терминал
- `cointcore:trader` («Торговый терминал»): `shop/ShopBlocks`, `shop/TraderBlock` (HorizontalDirectionalBlock, FACING, VoxelShape, прочность 3.5, кирка).
- Ресурсы: `assets/cointcore/models/block/trader.json`, текстура `cointcore:block/trader`, иконка глюона `textures/gui/gluon.png`.
- Офферы: `config/cointcore/trader_offers.json`. GUI: `TraderMenu` + `TraderScreen`. Сделки `TraderTrades` / `TraderTradePayload`. История цен — SavedData, спарклайн.
- Комиссия: `Commission.of`. Покупатель платит цена+комиссия, продавец получает цена−комиссия. Движения `trader_buy` / `trader_sell` (один игрок в deltas).

## Автомат игрока `cointcore:player_trader` («Торговый автомат»)
- Модель/текстура те же, что у терминала. BlockEntity: owner UUID+name (ломка/взрыв), склад 27 слотов только до миграции. После первого load офферы+склад уходят в глобальный рынок.
- **Глобальный рынок:** overworld SavedData `cointcore_global_market`. Любой автомат показывает все лоты сервера. Предметы в эскроу (точные стеки с components), не в инвентаре блока.
- Лот: seller UUID+name, стек (count = за сделку), число сделок, цена ≥ 1, created_at, expires_at. Лимит лотов / срок / плата за выставление / blacklist — `trader_offers.json`.
- Покупка атомарно: сначала снять эскроу, потом debit покупателя (цена+комиссия `player_shop_commission_percent`), credit продавца (офлайн ок), выдать предметы (лишнее — drop). Outbox `player_shop_buy` с deltas обоих. Скупка (`player_shop_sell` / sell-кнопка) с рынка снята; код `PlayerTraderDeals.sell` оставлен для компиляции.
- Вкладка «Мои лоты»: свои лоты, выставить (ghost из инвентаря, count/deals/price), снять (остаток в инвентарь/drop), статистика продаж, «вернуть» просроченное (return box, офлайн).
- Миграция: sell-офферы с buyPrice≥1 + совпадающий склад → глобальные лоты владельца; остаток склада и скупка-only → return box. Флаг `market_migrated` на BE.
- Крафт как раньше. Защита блока: ломать только владелец/admin. Выставлять/покупать — `cointcore.playershop.use`. Отмена чужого лота — только admin.
- GUI ванильное, рамки слотов только под реальными слотами (ghost + инвентарь), RU/EN без обрезки (`font.width`), 1080p scale 3. Иконка `textures/gui/gluon.png`.

## Сайт / AzLink
- Outbox: pay, trader_buy, trader_sell, admin_set, admin_add, **player_shop_buy, player_shop_sell**, site_*.
- `player_shop_*` — `isServerWalletLog() == true` (сайт логирует, баланс сайта меняет deltas как у pay).

## Тесты
PlayerShopValidationTest, PlayerShopManagementPolicyTest, GlobalMarketMathTest (+ прежние shop-тесты). `./gradlew build`.
