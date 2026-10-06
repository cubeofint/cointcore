package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.adastra.AdAstraGravityHandlerPatch;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class CointCoreMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> CREATE_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.create.CreateClaimBootstrapMixin",
            "com.mawlee.cointcore.mixin.create.SuperGlueSelectionPacketMixin",
            "com.mawlee.cointcore.mixin.create.ContraptionRemoveBlocksMixin",
            "com.mawlee.cointcore.mixin.create.ContraptionAddBlocksMixin",
            "com.mawlee.cointcore.mixin.create.BlockHelperDestroyMixin",
            "com.mawlee.cointcore.mixin.create.CartAssemblerAssembleMixin",
            "com.mawlee.cointcore.mixin.create.DeployerHandlerMixin",
            "com.mawlee.cointcore.mixin.create.ContraptionEntityChunkMixin",
            "com.mawlee.cointcore.mixin.create.CarriageContraptionEntityMixin"
    );

    private static final Set<String> QUARRY_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.quarryplus.SoftBlockChainBreakMixin"
    );

    private static final Set<String> ARS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.arsnouveau.EffectBreakMixin",
            "com.mawlee.cointcore.mixin.arsnouveau.EffectBurstMixin",
            "com.mawlee.cointcore.mixin.arsnouveau.EffectCrushMixin",
            "com.mawlee.cointcore.mixin.arsnouveau.EffectOrbitMixin",
            "com.mawlee.cointcore.mixin.arsnouveau.EntityOrbitProjectileMixin",
            "com.mawlee.cointcore.mixin.arsnouveau.EffectTossMixin",
            "com.mawlee.cointcore.mixin.arsnouveau.EffectWololoMixin",
            "com.mawlee.cointcore.mixin.arsnouveau.EntityLingeringSpellMixin",
            "com.mawlee.cointcore.mixin.arsnouveau.EntityWallSpellMixin"
    );

    private static final Set<String> NOT_ENOUGH_GLYPHS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.notenoughglyphs.EffectChainingMixin",
            "com.mawlee.cointcore.mixin.notenoughglyphs.PropagatePlaneMixin",
            "com.mawlee.cointcore.mixin.notenoughglyphs.TrailingProjectileMixin"
    );

    private static final Set<String> ARS_UNIFICATION_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.arsunification.DatapackSyncRecipeMixin"
    );

    private static final Set<String> COMPUTERCRAFT_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.computercraft.TurtleToolMixin"
    );

    private static final Set<String> INDUSTRIAL_FOREGOING_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.industrialforegoing.BlockUtilsClaimMixin",
            "com.mawlee.cointcore.mixin.industrialforegoing.MobDuplicatorLootMixin"
    );

    private static final Set<String> INDUSTRIAL_FOREGOING_SOULS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.industrialforegoingsouls.SoulSurgeBlockEntityMixin"
    );

    private static final Set<String> SFM_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.sfm.IntervalMixin",
            "com.mawlee.cointcore.mixin.sfm.ManagerBlockEntityMixin"
    );

    private static final Set<String> BOTANY_POTS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.botanypots.BotanyPotBlockEntityMixin"
    );

    private static final Set<String> DRACONIC_EVOLUTION_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.draconicevolution.ModularMiningToolMixin",
            "com.mawlee.cointcore.mixin.draconicevolution.StabilizedSpawnerLogicMixin",
            "com.mawlee.cointcore.mixin.draconicevolution.ProcessExplosionMixin",
            "com.mawlee.cointcore.mixin.draconicevolution.ExplosionHelperMixin",
            "com.mawlee.cointcore.mixin.draconicevolution.PlacedItemTickerMixin"
    );

    private static final Set<String> JUST_DIRE_THINGS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.justdirethings.BaseMachineBEMixin",
            "com.mawlee.cointcore.mixin.justdirethings.BlockBreakerT2BEMixin",
            "com.mawlee.cointcore.mixin.justdirethings.ClickerT2BEMixin",
            "com.mawlee.cointcore.mixin.justdirethings.FindSpotsToPlaceMixin",
            "com.mawlee.cointcore.mixin.justdirethings.FluidCollectorT2BEMixin",
            "com.mawlee.cointcore.mixin.justdirethings.BlockSwapperT2BEMixin",
            "com.mawlee.cointcore.mixin.justdirethings.SensorT2BEMixin",
            "com.mawlee.cointcore.mixin.justdirethings.MiscToolsTickAccelMixin",
            "com.mawlee.cointcore.mixin.justdirethings.TimeWandFakePlayerMixin",
            "com.mawlee.cointcore.mixin.justdirethings.PolymorphBossDenyMixin",
            "com.mawlee.cointcore.mixin.justdirethings.PolymorphicWandV2BossDenyMixin",
            "com.mawlee.cointcore.mixin.justdirethings.PortalEntityChunkLoadMixin"
    );

    private static final Set<String> POWAH_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.powah.PlayerTransmitterTileMixin"
    );

    private static final Set<String> ACTUALLY_ADDITIONS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.actuallyadditions.VerticalDiggerMixin"
    );

    private static final Set<String> ENTANGLED_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.entangled.EntangledBlockEntityMixin"
    );

    private static final Set<String> FTB_CHUNKS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.ftbchunks.PermissionsHelperBonusMixin",
            "com.mawlee.cointcore.mixin.ftbchunks.ClaimedChunkManagerBypassMixin"
    );

    private static final Set<String> FTB_RANKS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.ftbranks.RankManagerImplMixin"
    );

    private static final Set<String> FTB_TEAMS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.ftbteams.UpdatePropertiesRequestMixin",
            "com.mawlee.cointcore.mixin.ftbteams.TeamSettingsEditMixin"
    );

    private static final Set<String> FTB_ESSENTIALS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.ftbessentials.KitSaveStackMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.KitFromNbtMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.KitCommandCooldownResetMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.GiveMeKitCommandAliasMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.KitCommandAdminRenameMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.KitCommandUpdateMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.OfflineTeleportCommandMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.KitCreditClaimMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.KitCreditSuggestMixin"
    );

    private static final Set<String> EVILCRAFT_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.evilcraft.PacketCodecMixin",
            "com.mawlee.cointcore.mixin.evilcraft.SharedTankPacketCodecMixin",
            "com.mawlee.cointcore.mixin.evilcraft.UpdateWorldSharedTankPacketAccessor",
            "com.mawlee.cointcore.mixin.evilcraft.EntityVengeanceSpiritSpawnRandomMixin",
            "com.mawlee.cointcore.mixin.evilcraft.WeatherTypeMixin",
            "com.mawlee.cointcore.mixin.evilcraft.WeatherTypeLightningMixin",
            "com.mawlee.cointcore.mixin.evilcraft.BroomSmashMixin",
            "com.mawlee.cointcore.mixin.evilcraft.BroomKamikazeMixin",
            "com.mawlee.cointcore.mixin.evilcraft.EntityBroomMixin",
            "com.mawlee.cointcore.mixin.evilcraft.BroomMountedDigMixin",
            "com.mawlee.cointcore.mixin.evilcraft.EntangledChaliceAutofillMixin"
    );

    private static final Set<String> NATURESAURA_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.naturesaura.AuraChunkAccessor",
            "com.mawlee.cointcore.mixin.naturesaura.AuraChunkSpotMapMixin",
            "com.mawlee.cointcore.mixin.naturesaura.CommonEventsLevelTickMixin",
            "com.mawlee.cointcore.mixin.naturesaura.PlantBoostEffectMixin",
            "com.mawlee.cointcore.mixin.naturesaura.AnimalSpawnerLootMixin"
    );

    private static final Set<String> FORBIDDEN_ARCANUS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.forbiddenarcanus.HasEffectPreventMixin"
    );

    private static final Set<String> APOTHIC_SPAWNERS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.apothicspawners.StatModifierMixin",
            "com.mawlee.cointcore.mixin.apothicspawners.SpawnerLogicExtMixin"
    );

    private static final Set<String> AE2_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.ae2.TagSearchPredicateMixin",
            "com.mawlee.cointcore.mixin.ae2.SpatialStorageHelperMixin",
            "com.mawlee.cointcore.mixin.ae2.AEItemKeyFuzzyMaxCacheMixin",
            "com.mawlee.cointcore.mixin.ae2.ChunkLoadingServiceMixin"
    );

    private static final Set<String> REFINED_STORAGE_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.refinedstorage.ImporterNetworkNodeIdleSkipMixin"
    );

    private static final Set<String> DISCORD_CHAT_MOD_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.discord.MinecraftUtilsMixin",
            "com.mawlee.cointcore.mixin.discord.MinecraftEventsMixin"
    );

    private static final Set<String> MOB_GRINDING_UTILS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.mobgrindingutils.TileEntitySawMixin",
            "com.mawlee.cointcore.mixin.mobgrindingutils.MguSpawnerLootMixin"
    );

    private static final Set<String> RFTOOLS_BUILDER_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.rftoolsbuilder.BuilderTileEntityMixin",
            "com.mawlee.cointcore.mixin.rftoolsbuilder.BuilderDataMixin",
            "com.mawlee.cointcore.mixin.rftoolsbuilder.BuilderDataFlagsMixin"
    );

    private static final Set<String> FLUX_NETWORKS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.fluxnetworks.FluxPlayerDataMixin"
    );

    private static final Set<String> ORITECH_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.oritech.NuclearExplosionEntityMixin",
            "com.mawlee.cointcore.mixin.oritech.SpawnerControllerLootMixin"
    );

    private static final Set<String> ENDERIO_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.enderio.PoweredSpawnerLootMixin",
            "com.mawlee.cointcore.mixin.enderio.ItemConduitTickerMixin"
    );

    private static final Set<String> RFTOOLS_UTILITY_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.rftoolsutility.SpawnerTileLootMixin"
    );

    private static final Set<String> PNEUMATICCRAFT_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.pneumaticcraft.PressurizedSpawnerLootMixin"
    );

    private static final Set<String> MAHOU_TSUKAI_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.mahoutsukai.KeyPressPacketMixin"
    );

    private static final Set<String> ICE_AND_FIRE_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.iceandfire.IafDragonDestructionManagerMixin",
            "com.mawlee.cointcore.mixin.iceandfire.BlockLaunchExplosionMixin",
            "com.mawlee.cointcore.mixin.iceandfire.DragonBaseEntityMixin",
            "com.mawlee.cointcore.mixin.iceandfire.IafBoundingBoxBreakMixin"
    );

    private static final Set<String> TAB_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.tab.TabPlayerCanSeeMixin",
            "com.mawlee.cointcore.mixin.tab.NeoForgeTabPlayerVanishedMixin",
            "com.mawlee.cointcore.mixin.tab.TabPlayerListFormatMixin"
    );

    private static final Set<String> MODULAR_ROUTERS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.modularrouters.CompiledPlayerModuleMixin"
    );

    private static final Set<String> RELICS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.relics.RelicsVanishingRenderMixin"
    );

    /** Relics backpack scan needs both Relics and Sophisticated Backpacks. */
    private static final Set<String> RELICS_BACKPACK_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.relics.RelicsBackpackScanMixin"
    );

    private static final Set<String> COMPACT_MACHINES_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.compactmachines.ChunkLoaderUpgradeAppliedMixin"
    );

    private static final Set<String> HOSTILE_NETWORKS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.hostilenetworks.DataCenterTileEntityMixin"
    );

    private static final Set<String> IMMERSIVE_ENGINEERING_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.immersiveengineering.ChunkLoaderLogicMixin"
    );

    private static final Set<String> RAILCRAFT_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.railcraft.WorldSpikeBlockMixin",
            "com.mawlee.cointcore.mixin.railcraft.WorldSpikeMinecartMixin"
    );

    private static final Set<String> STEVES_CARTS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.stevescarts.ModuleChunkLoaderMixin"
    );

    private static final Set<String> ROOTS_CLASSIC_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.rootsclassic.RitualCauseRainMixin",
            "com.mawlee.cointcore.mixin.rootsclassic.RitualBanishRainMixin"
    );

    private static final Set<String> ALLTHEMODIUM_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.allthemodium.AtmThrownTridentMixin"
    );

    private static final Set<String> REGIONS_UNEXPLORED_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.regionsunexplored.RuMudBlockMixin"
    );

    private static final Set<String> MYSTICAL_AGRICULTURE_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.mysticalagriculture.SouliumSpawnerLootMixin"
    );

    private static final Set<String> AD_ASTRA_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.adastra.AdAstraGravityTickMixin",
            "com.mawlee.cointcore.mixin.adastra.AdAstraGravityListenersAccessor",
            "com.mawlee.cointcore.mixin.adastra.AdAstraOxygenTickMixin",
            "com.mawlee.cointcore.mixin.adastra.AdAstraTemperatureTickMixin",
            "com.mawlee.cointcore.mixin.adastra.AdAstraPlanetHandlerMixin",
            "com.mawlee.cointcore.mixin.adastra.AdAstraOxygenDistributorMixin",
            "com.mawlee.cointcore.mixin.adastra.AdAstraGravityNormalizerMixin",
            "com.mawlee.cointcore.mixin.adastra.AdAstraPipeBlockEntityMixin",
            "com.mawlee.cointcore.mixin.adastra.AdAstraChunkChangeMixin",
            "com.mawlee.cointcore.mixin.adastra.AdAstraEnvironmentTickMixin",
            "com.mawlee.cointcore.mixin.adastra.AdAstraDetectorMixin",
            "com.mawlee.cointcore.mixin.adastra.AdAstraAirVortexMixin"
    );

    /**
     * Compat mixin FQCNs → mod ids that must all be present. Core/vanilla mixins are absent and always apply.
     */
    private static final Map<String, List<String>> COMPAT_MIXIN_MODS = buildCompatMixinMods();

    private static Map<String, List<String>> buildCompatMixinMods() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        putAll(map, CREATE_MIXINS, "create");
        putAll(map, QUARRY_MIXINS, "quarryplus");
        putAll(map, ARS_MIXINS, "ars_nouveau");
        putAll(map, NOT_ENOUGH_GLYPHS_MIXINS, "not_enough_glyphs");
        putAll(map, ARS_UNIFICATION_MIXINS, "ars_unification");
        putAll(map, COMPUTERCRAFT_MIXINS, "computercraft");
        putAll(map, INDUSTRIAL_FOREGOING_MIXINS, "industrialforegoing");
        putAll(map, INDUSTRIAL_FOREGOING_SOULS_MIXINS, "industrialforegoingsouls");
        putAll(map, SFM_MIXINS, "sfm");
        putAll(map, BOTANY_POTS_MIXINS, "botanypots");
        putAll(map, DRACONIC_EVOLUTION_MIXINS, "draconicevolution");
        putAll(map, JUST_DIRE_THINGS_MIXINS, "justdirethings");
        putAll(map, POWAH_MIXINS, "powah");
        putAll(map, ACTUALLY_ADDITIONS_MIXINS, "actuallyadditions");
        putAll(map, ENTANGLED_MIXINS, "entangled");
        putAll(map, FTB_CHUNKS_MIXINS, "ftbchunks");
        putAll(map, FTB_RANKS_MIXINS, "ftbranks");
        putAll(map, FTB_TEAMS_MIXINS, "ftbteams");
        putAll(map, FTB_ESSENTIALS_MIXINS, "ftbessentials");
        putAll(map, EVILCRAFT_MIXINS, "evilcraft");
        putAll(map, NATURESAURA_MIXINS, "naturesaura");
        putAll(map, FORBIDDEN_ARCANUS_MIXINS, "forbidden_arcanus");
        putAll(map, APOTHIC_SPAWNERS_MIXINS, "apothic_spawners");
        putAll(map, AE2_MIXINS, "ae2");
        putAll(map, REFINED_STORAGE_MIXINS, "refinedstorage");
        putAll(map, DISCORD_CHAT_MOD_MIXINS, "discord_chat_mod");
        putAll(map, MOB_GRINDING_UTILS_MIXINS, "mob_grinding_utils");
        putAll(map, RFTOOLS_BUILDER_MIXINS, "rftoolsbuilder");
        putAll(map, FLUX_NETWORKS_MIXINS, "fluxnetworks");
        putAll(map, ORITECH_MIXINS, "oritech");
        putAll(map, ENDERIO_MIXINS, "enderio");
        putAll(map, RFTOOLS_UTILITY_MIXINS, "rftoolsutility");
        putAll(map, PNEUMATICCRAFT_MIXINS, "pneumaticcraft");
        putAll(map, MAHOU_TSUKAI_MIXINS, "mahoutsukai");
        putAll(map, ICE_AND_FIRE_MIXINS, "iceandfire");
        putAll(map, TAB_MIXINS, "tab");
        putAll(map, MODULAR_ROUTERS_MIXINS, "modularrouters");
        putAll(map, RELICS_MIXINS, "relics");
        putAll(map, RELICS_BACKPACK_MIXINS, "relics", "sophisticatedbackpacks");
        putAll(map, COMPACT_MACHINES_MIXINS, "compactmachines");
        putAll(map, HOSTILE_NETWORKS_MIXINS, "hostilenetworks");
        putAll(map, IMMERSIVE_ENGINEERING_MIXINS, "immersiveengineering");
        putAll(map, RAILCRAFT_MIXINS, "railcraft");
        putAll(map, STEVES_CARTS_MIXINS, "stevescarts");
        putAll(map, ROOTS_CLASSIC_MIXINS, "rootsclassic");
        putAll(map, ALLTHEMODIUM_MIXINS, "allthemodium");
        putAll(map, REGIONS_UNEXPLORED_MIXINS, "regions_unexplored");
        putAll(map, MYSTICAL_AGRICULTURE_MIXINS, "mysticalagriculture");
        putAll(map, AD_ASTRA_MIXINS, "ad_astra");
        return Map.copyOf(map);
    }

    private static void putAll(Map<String, List<String>> map, Set<String> mixins, String... modIds) {
        List<String> required = List.of(modIds);
        for (String mixin : mixins) {
            map.put(mixin, required);
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        List<String> requiredMods = COMPAT_MIXIN_MODS.get(mixinClassName);
        if (requiredMods == null) {
            return true;
        }

        List<String> missing = requiredMods.stream()
                .filter(modId -> !isModLoaded(modId))
                .toList();
        if (missing.isEmpty()) {
            return true;
        }

        LOGGER.warn(
                "Skipping compat mixin {} (target {}): missing mod(s) [{}]",
                shortMixinName(mixinClassName),
                targetClassName,
                missing.stream().collect(Collectors.joining(", "))
        );
        return false;
    }

    private static String shortMixinName(String mixinClassName) {
        String prefix = "com.mawlee.cointcore.mixin.";
        return mixinClassName.startsWith(prefix)
                ? mixinClassName.substring(prefix.length())
                : mixinClassName;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if ("com.mawlee.cointcore.mixin.adastra.AdAstraGravityTickMixin".equals(mixinClassName)) {
            AdAstraGravityHandlerPatch.install(targetClass);
        }
    }

    private static boolean isModLoaded(String modId) {
        return LoadingModList.get().getMods().stream()
                .map(ModInfo::getModId)
                .anyMatch(modId::equals);
    }
}
