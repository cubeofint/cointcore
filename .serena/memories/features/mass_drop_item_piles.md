# Массовые дропы и «кучи предметов» (item piles)

Проблема: ломание сундука с миллионами предметов (ваниль `Containers.dropItemStack` дробит по nextInt(21)+10) вешало сервер и клиент.

Решение (пакет `item/`):
- `mixin/minecraft/ContainersDropItemStackMixin` — HEAD-cancel на `Containers.dropItemStack(Level,DDD,ItemStack)` → `MassDropGuard.handle`.
- `MassDropGuard`: до мягкого бюджета на позицию — ваниль; дальше полные стаки с ванильным разбросом; сверх лимитов на позицию/тик уровня — в per-position `Overflow`, на `ServerTickEvent.Post` упаковывается в кучи (группы по `massDropPileMaxEntries`, ≤256 КиБ NBT).
- Куча = обычный `ItemEntity` (только в мире, никогда не предмет в инвентаре): показывает один стак, скрытый резерв `ItemPile` (поле через `ItemEntityPileMixin`, NBT `CointCorePile`), `ItemPiles.tick` доливает стак, имя `entity.cointcore.item_pile.name` («&6Куча предметов: &f%s»).
- Подбор vs уничтожение: `EntityLeaveLevelEvent` (синхронно внутри `discard()`); DISCARDED + пустой стак = подобрали → спавнится преемник; иначе лог `Item pile destroyed ...`. Кучи чистятся очисткой мира, горят, взрываются, тонут в лаве.
- `ItemEntitySleep` не старит предметы с age = -32768.
- Конфиг `item-perf.json`: `massDropGuard`=true, `massDropSoftEntitiesPerPos` 64, `massDropMaxEntitiesPerPos` 256, `massDropMaxEntitiesPerLevelTick` 1024, `massDropPileMaxEntries` 256.
- Отклонённый вариант: «ящик»-предмет (DropCrate) — пользователь запретил предметы, попадающие в инвентарь.
- Не выяснено: причина лога уничтожения кучи на 8 396 423 предмета (предлагалось логировать removal reason).