package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.adastra.AdAstraGravityHandlerPatch;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class CointCoreMixinPlugin implements IMixinConfigPlugin {
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
            "com.mawlee.cointcore.mixin.justdirethings.PolymorphicWandV2BossDenyMixin"
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

    private static final Set<String> AE2WTLIB_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.ae2wtlib.MagnetHandlerThrottleMixin"
    );

    private static final Set<String> AE2_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.ae2.TagSearchPredicateMixin",
            "com.mawlee.cointcore.mixin.ae2.SpatialStorageHelperMixin",
            "com.mawlee.cointcore.mixin.ae2.AEItemKeyFuzzyMaxCacheMixin"
    );

    private static final Set<String> ADVANCED_AE_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.advancedae.UpgradeCardsAutoStockMixin"
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
            "com.mawlee.cointcore.mixin.enderio.PoweredSpawnerLootMixin"
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

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (FTB_CHUNKS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("ftbchunks");
        }

        if (FTB_TEAMS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("ftbteams");
        }

        if (FTB_ESSENTIALS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("ftbessentials");
        }

        if (EVILCRAFT_MIXINS.contains(mixinClassName)) {
            return isModLoaded("evilcraft");
        }

        if (NATURESAURA_MIXINS.contains(mixinClassName)) {
            return isModLoaded("naturesaura");
        }

        if (FORBIDDEN_ARCANUS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("forbidden_arcanus");
        }

        if (APOTHIC_SPAWNERS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("apothic_spawners");
        }

        if (AE2WTLIB_MIXINS.contains(mixinClassName)) {
            return isModLoaded("ae2wtlib");
        }

        if (AE2_MIXINS.contains(mixinClassName)) {
            return isModLoaded("ae2");
        }

        if (ADVANCED_AE_MIXINS.contains(mixinClassName)) {
            return isModLoaded("advanced_ae");
        }

        if (REFINED_STORAGE_MIXINS.contains(mixinClassName)) {
            return isModLoaded("refinedstorage");
        }

        if (DISCORD_CHAT_MOD_MIXINS.contains(mixinClassName)) {
            return isModLoaded("discord_chat_mod");
        }

        if (MOB_GRINDING_UTILS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("mob_grinding_utils");
        }

        if (RFTOOLS_BUILDER_MIXINS.contains(mixinClassName)) {
            return isModLoaded("rftoolsbuilder");
        }

        if (FLUX_NETWORKS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("fluxnetworks");
        }

        if (ORITECH_MIXINS.contains(mixinClassName)) {
            return isModLoaded("oritech");
        }

        if (ENDERIO_MIXINS.contains(mixinClassName)) {
            return isModLoaded("enderio");
        }

        if (RFTOOLS_UTILITY_MIXINS.contains(mixinClassName)) {
            return isModLoaded("rftoolsutility");
        }

        if (PNEUMATICCRAFT_MIXINS.contains(mixinClassName)) {
            return isModLoaded("pneumaticcraft");
        }

        if (MAHOU_TSUKAI_MIXINS.contains(mixinClassName)) {
            return isModLoaded("mahoutsukai");
        }

        if (ICE_AND_FIRE_MIXINS.contains(mixinClassName)) {
            return isModLoaded("iceandfire");
        }

        if (TAB_MIXINS.contains(mixinClassName)) {
            return isModLoaded("tab");
        }

        if (MODULAR_ROUTERS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("modularrouters");
        }

        if (RELICS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("relics");
        }

        if (ROOTS_CLASSIC_MIXINS.contains(mixinClassName)) {
            return isModLoaded("rootsclassic");
        }

        if (ALLTHEMODIUM_MIXINS.contains(mixinClassName)) {
            return isModLoaded("allthemodium");
        }

        if (REGIONS_UNEXPLORED_MIXINS.contains(mixinClassName)) {
            return isModLoaded("regions_unexplored");
        }

        if (MYSTICAL_AGRICULTURE_MIXINS.contains(mixinClassName)) {
            return isModLoaded("mysticalagriculture");
        }

        if (AD_ASTRA_MIXINS.contains(mixinClassName)) {
            return isModLoaded("ad_astra");
        }

        if (CREATE_MIXINS.contains(mixinClassName)) {
            return isModLoaded("create");
        }

        if (ENTANGLED_MIXINS.contains(mixinClassName)) {
            return isModLoaded("entangled");
        }

        if (QUARRY_MIXINS.contains(mixinClassName)) {
            return isModLoaded("quarryplus");
        }

        if (ARS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("ars_nouveau");
        }

        if (NOT_ENOUGH_GLYPHS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("not_enough_glyphs");
        }

        if (ARS_UNIFICATION_MIXINS.contains(mixinClassName)) {
            return isModLoaded("ars_unification");
        }

        if (COMPUTERCRAFT_MIXINS.contains(mixinClassName)) {
            return isModLoaded("computercraft");
        }

        if (INDUSTRIAL_FOREGOING_MIXINS.contains(mixinClassName)) {
            return isModLoaded("industrialforegoing");
        }

        if (INDUSTRIAL_FOREGOING_SOULS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("industrialforegoingsouls");
        }

        if (SFM_MIXINS.contains(mixinClassName)) {
            return isModLoaded("sfm");
        }

        if (BOTANY_POTS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("botanypots");
        }

        if (DRACONIC_EVOLUTION_MIXINS.contains(mixinClassName)) {
            return isModLoaded("draconicevolution");
        }

        if (JUST_DIRE_THINGS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("justdirethings");
        }

        if (POWAH_MIXINS.contains(mixinClassName)) {
            return isModLoaded("powah");
        }

        if (ACTUALLY_ADDITIONS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("actuallyadditions");
        }

        return true;
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
