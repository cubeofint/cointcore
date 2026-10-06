package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.afk.AfkListMarker;
import com.mawlee.cointcore.afk.AfkTracker;
import com.mawlee.cointcore.vanish.VanishListMarker;
import com.mawlee.cointcore.vanish.VanishManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.commands.ListPlayersCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Mixin(ListPlayersCommand.class)
public abstract class ListPlayersCommandMixin {
    @Inject(method = "format", at = @At("HEAD"), cancellable = true)
    private static void cointcore$filterVanishedPlayers(
            CommandSourceStack source,
            Function<ServerPlayer, Component> nameExtractor,
            CallbackInfoReturnable<Integer> cir
    ) {
        PlayerList playerList = source.getServer().getPlayerList();
        ServerPlayer viewer = source.getPlayer();
        List<ServerPlayer> visible = new ArrayList<>();
        for (ServerPlayer player : playerList.getPlayers()) {
            if (viewer == null || !VanishManager.shouldHideFrom(player, viewer)) {
                visible.add(player);
            }
        }

        Component names = ComponentUtils.formatList(visible, player -> decorateName(nameExtractor.apply(player), player));
        source.sendSuccess(
                () -> Component.translatable("commands.list.players", visible.size(), playerList.getMaxPlayers(), names),
                false
        );
        cir.setReturnValue(visible.size());
    }

    private static Component decorateName(Component name, ServerPlayer player) {
        Component decorated = name;
        if (VanishManager.isVanished(player)) {
            decorated = VanishListMarker.append(decorated);
        }
        if (AfkTracker.isMarked(player)) {
            decorated = AfkListMarker.append(decorated);
        }
        return decorated;
    }
}
