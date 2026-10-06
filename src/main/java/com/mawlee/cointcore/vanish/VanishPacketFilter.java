package com.mawlee.cointcore.vanish;

import com.mawlee.cointcore.mixin.accessor.ClientboundPlayerInfoUpdatePacketAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class VanishPacketFilter {
    private static final int SHARED_FLAGS_DATA_ID = 0;
    private static final int GLOWING_FLAG_BIT = 6;

    private VanishPacketFilter() {
    }

    public static Packet<?> sanitize(MinecraftServer server, Packet<?> packet) {
        if (!(packet instanceof ClientboundSetEntityDataPacket entityDataPacket)) {
            return packet;
        }

        ServerPlayer vanished = findVanishedPlayer(server, entityDataPacket.id());
        if (vanished == null) {
            return packet;
        }

        List<SynchedEntityData.DataValue<?>> items = entityDataPacket.packedItems();
        List<SynchedEntityData.DataValue<?>> modified = null;

        for (int i = 0; i < items.size(); i++) {
            SynchedEntityData.DataValue<?> item = items.get(i);
            if (item.id() == SHARED_FLAGS_DATA_ID && item.value() instanceof Byte flags) {
                byte stripped = (byte) (flags & ~(1 << GLOWING_FLAG_BIT));
                if (stripped != flags) {
                    if (modified == null) {
                        modified = new ArrayList<>(items);
                    }
                    modified.set(i, new SynchedEntityData.DataValue<>(item.id(), EntityDataSerializers.BYTE, stripped));
                }
            }
        }

        if (modified == null) {
            return packet;
        }

        return new ClientboundSetEntityDataPacket(entityDataPacket.id(), modified);
    }

    public static boolean shouldCancel(MinecraftServer server, ServerPlayer receiver, Packet<?> packet) {
        if (packet instanceof ClientboundPlayerInfoUpdatePacket infoPacket) {
            return filterPlayerInfo(infoPacket, receiver);
        }

        if (packet instanceof ClientboundSystemChatPacket chatPacket) {
            return shouldSuppressChat(server, receiver, chatPacket);
        }

        if (packet instanceof ClientboundTakeItemEntityPacket pickupPacket) {
            Entity picker = receiver.level().getEntity(pickupPacket.getPlayerId());
            return picker instanceof ServerPlayer vanished && VanishVisibility.isHiddenFrom(receiver, vanished);
        }

        if (packet instanceof ClientboundAnimatePacket animatePacket) {
            Entity entity = receiver.level().getEntity(animatePacket.getId());
            return entity instanceof ServerPlayer vanished && VanishVisibility.isHiddenFrom(receiver, vanished);
        }

        if (packet instanceof ClientboundHurtAnimationPacket hurtPacket) {
            Entity entity = receiver.level().getEntity(hurtPacket.id());
            return entity instanceof ServerPlayer vanished && VanishVisibility.isHiddenFrom(receiver, vanished);
        }

        if (packet instanceof ClientboundSetEntityDataPacket entityDataPacket) {
            Entity entity = receiver.level().getEntity(entityDataPacket.id());
            return entity instanceof ServerPlayer vanished && VanishVisibility.isHiddenFrom(receiver, vanished);
        }

        if (packet instanceof ClientboundUpdateMobEffectPacket effectPacket) {
            Entity entity = receiver.level().getEntity(effectPacket.getEntityId());
            if (entity instanceof ServerPlayer vanished && VanishManager.isVanished(vanished)) {
                if (effectPacket.getEffect().is(MobEffects.GLOWING)) {
                    return true;
                }
            }
            return entity instanceof ServerPlayer vanished && VanishVisibility.isHiddenFrom(receiver, vanished);
        }

        var playerList = server.getPlayerList();
        Player directSource = null;

        if (packet instanceof ClientboundSoundPacket soundPacket) {
            directSource = VanishInteractionTracker.resolveBroadcastSource(packet, playerList);
            return VanishInteractionTracker.findHiddenCause(
                    directSource,
                    receiver.level(),
                    new Vec3(soundPacket.getX(), soundPacket.getY(), soundPacket.getZ()),
                    receiver
            ) != null;
        }

        if (packet instanceof ClientboundSoundEntityPacket soundEntityPacket) {
            directSource = VanishInteractionTracker.resolveBroadcastSource(packet, playerList);
            Entity soundEntity = receiver.level().getEntity(soundEntityPacket.getId());
            return VanishInteractionTracker.findHiddenCause(directSource, receiver.level(), soundEntity, receiver) != null;
        }

        if (packet instanceof ClientboundLevelEventPacket levelEventPacket) {
            directSource = VanishInteractionTracker.resolveBroadcastSource(packet, playerList);
            return VanishInteractionTracker.findHiddenCause(
                    directSource,
                    receiver.level(),
                    Vec3.atCenterOf(levelEventPacket.getPos()),
                    receiver
            ) != null;
        }

        if (packet instanceof ClientboundBlockEventPacket blockEventPacket) {
            return VanishInteractionTracker.findHiddenCauseForBlock(
                    receiver.level(),
                    blockEventPacket.getPos(),
                    receiver
            ) != null;
        }

        if (packet instanceof ClientboundLevelParticlesPacket particlesPacket) {
            return VanishInteractionTracker.findHiddenCause(
                    null,
                    receiver.level(),
                    new Vec3(particlesPacket.getX(), particlesPacket.getY(), particlesPacket.getZ()),
                    receiver
            ) != null;
        }

        if (packet instanceof ClientboundBlockDestructionPacket destructionPacket) {
            return VanishInteractionTracker.findHiddenCause(
                    null,
                    receiver.level(),
                    Vec3.atCenterOf(destructionPacket.getPos()),
                    receiver
            ) != null;
        }

        return false;
    }

    private static ServerPlayer findVanishedPlayer(MinecraftServer server, int entityId) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getId() == entityId && VanishManager.isVanished(player)) {
                return player;
            }
        }
        return null;
    }

    private static boolean filterPlayerInfo(ClientboundPlayerInfoUpdatePacket infoPacket, ServerPlayer receiver) {
        List<ClientboundPlayerInfoUpdatePacket.Entry> filtered = new ArrayList<>();
        boolean changed = false;

        for (ClientboundPlayerInfoUpdatePacket.Entry entry : infoPacket.entries()) {
            if (shouldHidePlayer(entry.profileId(), receiver)) {
                changed = true;
                continue;
            }

            ClientboundPlayerInfoUpdatePacket.Entry decorated = decorateVanishedEntry(entry, receiver);
            changed |= decorated != entry;
            filtered.add(decorated);
        }

        if (filtered.isEmpty()) {
            return true;
        }

        if (changed) {
            ((ClientboundPlayerInfoUpdatePacketAccess) infoPacket).cointcore$setEntries(filtered);
        }

        return false;
    }

    private static ClientboundPlayerInfoUpdatePacket.Entry decorateVanishedEntry(
            ClientboundPlayerInfoUpdatePacket.Entry entry,
            ServerPlayer receiver
    ) {
        ServerPlayer subject = receiver.server.getPlayerList().getPlayer(entry.profileId());
        if (subject == null || !VanishManager.isVanished(subject)) {
            return entry;
        }

        Component displayName = entry.displayName();
        if (displayName == null) {
            displayName = subject.getTabListDisplayName();
        }
        if (displayName == null) {
            displayName = subject.getDisplayName();
        }
        Component decorated = VanishListMarker.append(displayName);
        if (decorated.equals(entry.displayName())) {
            return entry;
        }

        return new ClientboundPlayerInfoUpdatePacket.Entry(
                entry.profileId(),
                entry.profile(),
                entry.listed(),
                entry.latency(),
                entry.gameMode(),
                decorated,
                entry.chatSession()
        );
    }

    private static boolean shouldHidePlayer(UUID profileId, ServerPlayer receiver) {
        ServerPlayer subject = receiver.server.getPlayerList().getPlayer(profileId);
        if (subject == null) {
            return VanishManager.shouldHideFrom(profileId, receiver);
        }
        return VanishVisibility.isHiddenFrom(receiver, subject);
    }

    private static boolean shouldSuppressChat(MinecraftServer server, ServerPlayer receiver, ClientboundSystemChatPacket chatPacket) {
        if (!(chatPacket.content() instanceof MutableComponent component)
                || !(component.getContents() instanceof TranslatableContents contents)) {
            return false;
        }

        List<ServerPlayer> vanishedSubjects = new ArrayList<>();

        if (VanishFieldHolder.joiningPlayer != null && VanishManager.isVanished(VanishFieldHolder.joiningPlayer)) {
            vanishedSubjects.add(VanishFieldHolder.joiningPlayer);
        }

        if (VanishFieldHolder.leavingPlayer != null && VanishManager.isVanished(VanishFieldHolder.leavingPlayer)) {
            vanishedSubjects.add(VanishFieldHolder.leavingPlayer);
        }

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (VanishManager.isVanished(online)) {
                vanishedSubjects.add(online);
            }
        }

        String key = contents.getKey();

        if (key.startsWith("death.")) {
            if (contents.getArgs().length > 0 && contents.getArgs()[0] instanceof Component playerName) {
                for (ServerPlayer vanished : vanishedSubjects) {
                    if (vanished.getDisplayName().getString().equals(playerName.getString())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
