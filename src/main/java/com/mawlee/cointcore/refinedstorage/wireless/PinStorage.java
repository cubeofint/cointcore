package com.mawlee.cointcore.refinedstorage.wireless;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class PinStorage {
    private static PinBoard<PlatformResourceKey> board = new PinBoard<>();
    private static Path file;
    private static boolean writable, loaded;
    private static long epoch;
    private PinStorage() { }
    public static PinBoard<PlatformResourceKey> board() { load(); return board; }
    public static long epoch() { return epoch; }

    public static void load() {
        var minecraft = Minecraft.getInstance();
        if (loaded || minecraft.player == null || minecraft.level == null) return;
        loaded = true;
        writable = false;
        try {
            String server = minecraft.getCurrentServer() == null
                    ? "local:" + (minecraft.getSingleplayerServer() == null ? "world" : minecraft.getSingleplayerServer().getWorldData().getLevelName())
                    : minecraft.getCurrentServer().ip;
            String identity = server + ":" + minecraft.player.getUUID();
            String key = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)));
            file = FMLPaths.CONFIGDIR.get().resolve("cointcore/refined-pins/" + key + ".json");
            var ops = minecraft.level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            var legacy = FMLPaths.CONFIGDIR.get().resolve("cointcore/refined-favorites/" + key + ".json");
            if (Files.exists(file)) {
                board = PinBoardJson.read(JsonParser.parseString(Files.readString(file)).getAsJsonObject(),
                        value -> ResourceCodecs.CODEC.parse(ops, value).result());
            } else if (Files.exists(legacy)) {
                // Preserve the previous favorites file as a backup; flatten its items into manual pins.
                var root = JsonParser.parseString(Files.readString(legacy)).getAsJsonObject();
                for (var group : root.entrySet()) for (var value : group.getValue().getAsJsonArray())
                    ResourceCodecs.CODEC.parse(ops, value).result().ifPresent(board::addManual);
            }
            epoch++;
            writable = true;
            if (!Files.exists(file) && Files.exists(legacy)) save();
        } catch (Exception error) {
            LoggerFactory.getLogger(PinStorage.class).warn("Cannot load RS pins; preserving the existing profile", error);
        }
    }

    public static void reset() { loaded = false; file = null; writable = false; board = new PinBoard<>(); epoch++; }
    public static void save() {
        if (!writable || file == null || Minecraft.getInstance().level == null) return;
        try {
            var ops = Minecraft.getInstance().level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            var root = PinBoardJson.write(board, key -> ResourceCodecs.CODEC.encodeStart(ops, key).getOrThrow());
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception error) {
            LoggerFactory.getLogger(PinStorage.class).warn("Cannot save RS pins", error);
        }
    }
}
