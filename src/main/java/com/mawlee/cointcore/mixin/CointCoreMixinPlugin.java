package com.mawlee.cointcore.mixin;

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
            "com.mawlee.cointcore.mixin.arsnouveau.EffectBreakMixin"
    );

    private static final Set<String> COMPUTERCRAFT_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.computercraft.TurtleToolMixin"
    );

    private static final Set<String> INDUSTRIAL_FOREGOING_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.industrialforegoing.BlockUtilsClaimMixin"
    );

    private static final Set<String> DRACONIC_EVOLUTION_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.draconicevolution.ModularMiningToolMixin"
    );

    private static final Set<String> ACTUALLY_ADDITIONS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.actuallyadditions.VerticalDiggerMixin"
    );

    private static final Set<String> ENTANGLED_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.entangled.EntangledBlockEntityMixin"
    );

    private static final Set<String> FTB_ESSENTIALS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.ftbessentials.KitSaveStackMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.KitFromNbtMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.KitCommandCooldownResetMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.GiveMeKitCommandAliasMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.KitCommandAdminRenameMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.OfflineTeleportCommandMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.KitCreditClaimMixin",
            "com.mawlee.cointcore.mixin.ftbessentials.KitCreditSuggestMixin"
    );

    private static final Set<String> EVILCRAFT_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.evilcraft.PacketCodecMixin"
    );

    private static final Set<String> APOTHIC_SPAWNERS_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.apothicspawners.StatModifierMixin",
            "com.mawlee.cointcore.mixin.apothicspawners.SpawnerLogicExtMixin"
    );

    private static final Set<String> AE2_MIXINS = Set.of(
            "com.mawlee.cointcore.mixin.ae2.TagSearchPredicateMixin",
            "com.mawlee.cointcore.mixin.ae2.SpatialStorageHelperMixin"
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
        if (FTB_ESSENTIALS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("ftbessentials");
        }

        if (EVILCRAFT_MIXINS.contains(mixinClassName)) {
            return isModLoaded("evilcraft");
        }

        if (APOTHIC_SPAWNERS_MIXINS.contains(mixinClassName)) {
            return isModLoaded("apothic_spawners");
        }

        if (AE2_MIXINS.contains(mixinClassName)) {
            return isModLoaded("ae2");
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

        if (COMPUTERCRAFT_MIXINS.contains(mixinClassName)) {
            return isModLoaded("computercraft");
        }

        if (INDUSTRIAL_FOREGOING_MIXINS.contains(mixinClassName)) {
            return isModLoaded("industrialforegoing");
        }

        if (DRACONIC_EVOLUTION_MIXINS.contains(mixinClassName)) {
            return isModLoaded("draconicevolution");
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
    }

    private static boolean isModLoaded(String modId) {
        return LoadingModList.get().getMods().stream()
                .map(ModInfo::getModId)
                .anyMatch(modId::equals);
    }
}
