package com.mawlee.cointcore.mixin.neovitae;

import com.mawlee.cointcore.config.NeovitaeMissingMappingsConfig;
import com.mawlee.cointcore.neovitae.NeovitaePaletteRewrite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Soft mixin: only applied when {@code neovitae} is loaded. Replaces
 * {@code NVMissingMappings#rewriteSectionPalettes} with a HashMap lookup and
 * skips sections whose palette has no remapped ids.
 *
 * <p>Method verified against neovitae-1.21.1-1.1.38 (ATM10 8.3) and 1.1.44.
 * NeoVitae {@code neovitae-server.toml} has no option to disable this rewrite.
 */
@Pseudo
@Mixin(
        targets = "com.breakinblocks.neovitae.common.event.NVMissingMappings",
        remap = false
)
public abstract class NVMissingMappingsMixin {
    @Shadow
    @Final
    private static String[][] BLOCK_REMAPS;

    @Unique
    private static volatile Map<String, String> cointcore$remapCache;

    @Inject(method = "rewriteSectionPalettes", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$fastRewriteSectionPalettes(CompoundTag root, CallbackInfo ci) {
        if (!NeovitaeMissingMappingsConfig.isEnabled()) {
            return;
        }
        ci.cancel();
        if (root == null || !root.contains("sections", Tag.TAG_LIST)) {
            return;
        }
        Map<String, String> remaps = cointcore$remaps();
        if (remaps.isEmpty()) {
            return;
        }

        ListTag sections = root.getList("sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < sections.size(); i++) {
            CompoundTag section = sections.getCompound(i);
            if (!section.contains("block_states", Tag.TAG_COMPOUND)) {
                continue;
            }
            CompoundTag blockStates = section.getCompound("block_states");
            if (!blockStates.contains("palette", Tag.TAG_LIST)) {
                continue;
            }
            ListTag palette = blockStates.getList("palette", Tag.TAG_COMPOUND);
            boolean needsRewrite = false;
            for (int p = 0; p < palette.size(); p++) {
                if (remaps.containsKey(palette.getCompound(p).getString("Name"))) {
                    needsRewrite = true;
                    break;
                }
            }
            if (!needsRewrite) {
                continue;
            }
            for (int p = 0; p < palette.size(); p++) {
                CompoundTag entry = palette.getCompound(p);
                String remapped = remaps.get(entry.getString("Name"));
                if (remapped != null) {
                    entry.putString("Name", remapped);
                }
            }
        }
    }

    @Unique
    private static Map<String, String> cointcore$remaps() {
        Map<String, String> cached = cointcore$remapCache;
        if (cached != null) {
            return cached;
        }
        Map<String, String> built = NeovitaePaletteRewrite.remapTable(BLOCK_REMAPS);
        cointcore$remapCache = built;
        return built;
    }
}
