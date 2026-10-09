# Chunk / team limits (package `chunklimit`)

Config: `config/cointcore/chunk-limits.json` (enabled, blockLimits, groups, entityLimits, teamBlockLimits, teamGroups, teamEntityLimits). Reload: `/cointcore reload`; edits: `/cointcore chunklimit ...` (node `cointcore.chunklimit`, OP). Bypass: `cointcore.chunklimit.bypass` (FakePlayer never bypasses for blocks).

## Key resolution (ONE key per object per scope, most specific wins; limits not cumulative)
- Blocks: exact id / group member (`block:`/`group:`; exact beats group, first group wins) -> first matching `#tag` (`tag:`) -> `mod:*` (`mod:`). Cached per Block, cache cleared on any change.
- Entities: exact id (`entity:`, any entity incl. non-mobs) -> `mod:*` (`emod:`, Mob only) -> `*` cap (`mob:cap`, Mob only).
- Chunk scope and team scope (FTB claim owner) are checked independently.

## Blocks
- `ChunkLimitIndex`: in-memory per-chunk TreeSet<BlockPos> per key id; full 16^3 scan on chunk load; updated by `LevelSetBlockChunkLimitMixin` (HEAD/RETURN of Level.setBlock); dropped on unload.
- `TeamLimitIndex`: per-chunk contribution -> per-team totals; kept across unload, NOT persisted (restart = only loaded chunks), wiped by any config change.
- Enforcement only for player/FakePlayer placement: RightClickBlock pre-check (BlockItem, count+1) + EntityPlaceEvent post-check (cancel + return clone item). No other paths (pistons, Create, /setblock, worldgen) are limited. `shouldTickLimitedBlock` always true -> isActive/inert and SoulSurge `respectChunkLimits` are no-ops.

## Entities
- Chunk counts are live AABB queries (no index). Team counts via `TeamMobLimitIndex` (sync on join/leave/chunk load); in practice only loaded chunks count (leave on unload zeroes contribution).
- Paths: spawn egg/mob bucket right-click, FinalizeSpawn (natural/spawner), EntityJoin (everything else incl. breeding, portals, items), ItemToss, chunk-load cull (`cullOverLimit`, newest first, no drops, max 512/chunk).
- Exempt: farm spawners (SpawnerLoot), Sunken City / Cataclysm refills, players.

## Per-player block limits (added 2026-10-08, uncommitted until user asks)
- `LimitScope.PLAYER`, JSON `playerBlockLimits` / `playerGroups`, commands `/cointcore chunklimit player block|group ...`, `player check [player]`.
- Owner store: `BlockOwnerSavedData` (overworld data `cointcore_block_owners`) wrapping pure `BlockOwnerRegistry` (pos -> owner+blockId, per-owner counts by key; counts rebuild lazily via `ChunkLimitConfig.rulesVersion()`).
- Owner recorded at `EntityPlaceEvent` LOWEST (real players only, block must have a player key); removed on any block change; `validateChunk` on chunk load drops mismatches. Personal check always uses count+1 (owner recorded after event).
- Team limit stays claim-based (shared pool of all members building in team claims) — user semantics: player limit per person, team limit shared; team checked before player.

## Fixed 2026-10-08
- Commands: id/tagId/blockId args now `ResourceLocationArgument` (StringArgumentType rejected `:`; workaround was quotes).
- Dupe: `ItemPlacementGuard` + `mixin/neoforge/CommonHooksItemPlacementMixin`; clone returned only outside `onPlaceItemIntoWorld`.
- Index: `LevelSetBlockChunkLimitMixin` replaced by `LevelChunkSetBlockStateChunkLimitMixin` (old state from return value, current state re-read); `bucketFor` full-rescans a loaded chunk instead of creating a partial bucket.

## Remaining known issues (not fixed)
3. Team indexes not persisted / wiped on config edit -> team limits undercount.
4. ItemToss cancel deletes the item (NeoForge semantics), deny only sends a message.
5. FinalizeSpawn cancels without checking bypass -> bypass does not work for spawn eggs at the limit.
6. Mobs entering via portal / breeding are silently discarded; cull on chunk load silently removes tamed/named mobs.
