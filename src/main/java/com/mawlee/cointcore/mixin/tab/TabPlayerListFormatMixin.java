package com.mawlee.cointcore.mixin.tab;

import com.mawlee.cointcore.afk.AfkListMarker;
import com.mawlee.cointcore.afk.AfkTracker;
import com.mawlee.cointcore.vanish.VanishListMarker;
import com.mawlee.cointcore.vanish.VanishManager;
import me.neznamy.tab.shared.chat.component.TabComponent;
import me.neznamy.tab.shared.chat.component.TabTextComponent;
import me.neznamy.tab.shared.features.playerlist.PlayerList;
import me.neznamy.tab.shared.platform.TabPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerList.class, remap = false)
public abstract class TabPlayerListFormatMixin {
    @Inject(method = "getTabFormat", at = @At("RETURN"), cancellable = true, remap = false)
    private void cointcore$appendStatusTags(
            TabPlayer formatted,
            TabPlayer viewer,
            CallbackInfoReturnable<TabComponent> cir
    ) {
        TabComponent original = cir.getReturnValue();
        if (original == null || formatted == null) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(formatted.getUniqueId());
        if (player == null) {
            return;
        }

        boolean vanished = VanishManager.isVanished(player);
        boolean afk = AfkTracker.isMarked(player);
        if (!vanished && !afk) {
            return;
        }

        String legacy = original.toLegacyText();
        TabComponent tagged = new TabTextComponent("");
        tagged.addExtra(original);

        if (vanished && !legacy.contains(VanishListMarker.TAG)) {
            tagged.addExtra(TabComponent.fromColoredText("&7 " + VanishListMarker.TAG));
            legacy = legacy + VanishListMarker.TAG;
        }
        if (afk && !legacy.contains(AfkListMarker.TAG)) {
            tagged.addExtra(TabComponent.fromColoredText("&7 " + AfkListMarker.TAG));
        }

        cir.setReturnValue(tagged);
    }
}
