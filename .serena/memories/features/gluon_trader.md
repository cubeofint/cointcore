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
- Модель/текстура те же, что у терминала. BlockEntity: owner UUID+name, склад 27 слотов (`SimpleContainer`, **не** Container на BE), список офферов, `lifetime_revenue` (статистика).
- Крафт (железо/стекло/сундук/редстоун), условие `cointcore:player_shop_crafting` ← `player_shop_crafting` в `trader_offers.json` (дефолт true). Также creative / give.
- Комиссия: `player_shop_commission_percent` (дефолт = системной).
- **Выручка не копится в блоке.** На сделке сразу: debit/credit кошельков (владелец может быть офлайн), типы `player_shop_buy` / `player_shop_sell` с deltas как у `/pay` (покупатель и владелец). Комиссия сжигается (разница totals). Кнопка «забрать выручку» не нужна.
- Владелец: вкладка «Управление» — склад, создание/правка/удаление офферов (ghost-слот, кол-во, цена продажи игроку и опционально скупки). Покупатель: то же GUI, что терминал, без спарклайна, «нет в наличии», имя владельца в заголовке.
- Защита: ломать/управление только владелец (UUID BlockEntity) или `cointcore.playershop.admin`. Каждый manage-пакет (вкладка, save/delete оффера, клики по складу) проверяется на сервере; клиентский флаг вкладки и «owner» не доверяются. Меню управления — отдельный `MenuType` `player_trader_manage`; покупатель получает только `player_trader` без слотов склада. Оффер хранит `seller_id` действующего игрока с сервера, не UUID владельца блока, если действует не владелец.
- Blast resistance 3600000; hoppers/pipes не видят склад. Права `cointcore.playershop.place` / `use` (всем по умолчанию).
- Сделки только сервер, match включая components, synchronized на BE. Выручка покупки идёт на `seller_id` оффера (fallback — владелец блока).

## Сайт / AzLink
- Outbox: pay, trader_buy, trader_sell, admin_set, admin_add, **player_shop_buy, player_shop_sell**, site_*.
- `player_shop_*` — `isServerWalletLog() == true` (сайт логирует, баланс сайта меняет deltas как у pay).

## Тесты
PlayerShopValidationTest, PlayerShopManagementPolicyTest (+ прежние shop-тесты). `./gradlew build`.
