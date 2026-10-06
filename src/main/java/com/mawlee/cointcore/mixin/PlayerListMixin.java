package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.ban.BanLoginGuard;
import com.mawlee.cointcore.invsee.InvSeeTargets;
import com.mawlee.cointcore.join.JoinLeaveMessageFilter;
import com.mawlee.cointcore.vanish.VanishFieldHolder;
import com.mawlee.cointcore.vanish.VanishInteractionTracker;
import com.mawlee.cointcore.vanish.VanishManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.Date;
import java.util.function.Function;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.players.StoredUserEntry;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void cointcore$beforePlaceNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        VanishManager.loadFromStorage(player);
        VanishFieldHolder.joiningPlayer = player;
    }

    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void cointcore$afterPlaceNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        VanishFieldHolder.joiningPlayer = null;
        InvSeeTargets.switchToOnline(player);
    }

    @Inject(method = "load(Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/nbt/CompoundTag;", at = @At("HEAD"))
    private void cointcore$flushInvSeeBeforeLoad(ServerPlayer player, CallbackInfoReturnable<CompoundTag> cir) {
        InvSeeTargets.flushBeforeLoad(player);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void cointcore$beforeRemove(ServerPlayer player, CallbackInfo ci) {
        VanishFieldHolder.leavingPlayer = player;
        InvSeeTargets.freezeForLogout(player);
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void cointcore$afterRemove(ServerPlayer player, CallbackInfo ci) {
        InvSeeTargets.switchToOfflineAfterSave(player);
        VanishFieldHolder.leavingPlayer = null;
    }

    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), cancellable = true)
    private void cointcore$suppressJoinLeaveBroadcast(Component message, boolean overlay, CallbackInfo ci) {
        if (JoinLeaveMessageFilter.isJoinOrLeaveMessage(message)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Ljava/util/function/Function;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cointcore$suppressJoinLeaveBroadcastWithFactory(
            Component message,
            Function<ServerPlayer, Component> playerMessageFactory,
            boolean overlay,
            CallbackInfo ci
    ) {
        if (JoinLeaveMessageFilter.isJoinOrLeaveMessage(message)) {
            ci.cancel();
        }
    }

    @Inject(method = "broadcast", at = @At("HEAD"))
    private void cointcore$rememberSoundSource(
            Player except,
            double x,
            double y,
            double z,
            double radius,
            ResourceKey<Level> dimension,
            Packet<?> packet,
            CallbackInfo ci
    ) {
        if (except != null && (packet instanceof ClientboundSoundPacket
                || packet instanceof ClientboundSoundEntityPacket
                || packet instanceof ClientboundLevelEventPacket
                || packet instanceof ClientboundBlockEventPacket)) {
            VanishInteractionTracker.rememberBroadcastSource(packet, except);
        }
    }

    @Inject(method = "canPlayerLogin", at = @At("HEAD"))
    private void cointcore$repairInconsistentBanState(
            SocketAddress address,
            GameProfile profile,
            CallbackInfoReturnable<Component> cir
    ) {
        UserBanList bans = ((PlayerList) (Object) this).getBans();
        if (bans.isBanned(profile) && BanLoginGuard.resolveEntry(bans, profile) == null) {
            BanLoginGuard.purgeInconsistentBan(bans, profile);
        }
    }

    @WrapOperation(
            method = "canPlayerLogin",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/UserBanList;get(Ljava/lang/Object;)Lnet/minecraft/server/players/StoredUserEntry;"
            )
    )
    private StoredUserEntry cointcore$resolveBanEntry(
            UserBanList instance,
            Object key,
            Operation<StoredUserEntry> original,
            SocketAddress address,
            GameProfile profile
    ) {
        StoredUserEntry entry = original.call(instance, key);
        if (entry != null) {
            return entry;
        }

        return BanLoginGuard.resolveEntry(instance, profile);
    }

    @Redirect(
            method = "canPlayerLogin",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/UserBanListEntry;getReason()Ljava/lang/String;"
            )
    )
    private String cointcore$safeBanReason(UserBanListEntry entry) {
        return BanLoginGuard.safeReason(entry);
    }

    @Redirect(
            method = "canPlayerLogin",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/UserBanListEntry;getExpires()Ljava/util/Date;"
            ),
            require = 0
    )
    private Date cointcore$safeBanExpires(UserBanListEntry entry) {
        return BanLoginGuard.safeExpires(entry);
    }
}
