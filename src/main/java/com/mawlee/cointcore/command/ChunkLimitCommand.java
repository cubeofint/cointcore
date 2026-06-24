package com.mawlee.cointcore.command;

import com.mawlee.cointcore.chunklimit.ChunkLimitService;
import com.mawlee.cointcore.chunklimit.EntityWorldDumpService;
import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.io.IOException;

public final class ChunkLimitCommand {
    private static final SimpleCommandExceptionType INVALID_ID = new SimpleCommandExceptionType(
            Component.literal("Invalid resource id")
    );
    private static final SimpleCommandExceptionType UNKNOWN_BLOCK = new SimpleCommandExceptionType(
            Component.literal("Unknown block id")
    );
    private static final SimpleCommandExceptionType UNKNOWN_ENTITY = new SimpleCommandExceptionType(
            Component.literal("Unknown entity type id")
    );
    private static final SimpleCommandExceptionType SAVE_FAILED = new SimpleCommandExceptionType(
            Component.literal("Failed to save chunk limit config")
    );
    private static final SimpleCommandExceptionType REQUIRES_PLAYER = new SimpleCommandExceptionType(
            Component.literal("Command must be run by a player")
    );
    private static final SimpleCommandExceptionType NO_HELD_BLOCK = new SimpleCommandExceptionType(
            Component.literal("Hold a block item in your main hand")
    );
    private static final SimpleCommandExceptionType DUMP_FAILED = new SimpleCommandExceptionType(
            Component.literal("Failed to write entity dump file")
    );

    private ChunkLimitCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> chunkLimitCommand() {
        return Commands.literal("chunklimit")
                .requires(ChunkLimitCommand::canManage)
                .then(Commands.literal("enabled")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ChunkLimitCommand::setEnabled)))
                .then(Commands.literal("block")
                        .then(Commands.literal("set")
                                .then(Commands.literal("held")
                                        .then(Commands.argument("limit", IntegerArgumentType.integer(0))
                                                .executes(ChunkLimitCommand::setHeldBlockLimit)))
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                BuiltInRegistries.BLOCK.keySet().stream(),
                                                builder
                                        ))
                                        .then(Commands.argument("limit", IntegerArgumentType.integer(0))
                                                .executes(ChunkLimitCommand::setBlockLimit))))
                        .then(Commands.literal("remove")
                                .then(Commands.literal("held")
                                        .executes(ChunkLimitCommand::removeHeldBlockLimit))
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                ChunkLimitConfig.getBlockLimits().keySet().stream(),
                                                builder
                                        ))
                                        .executes(ChunkLimitCommand::removeBlockLimit)))
                        .then(Commands.literal("list")
                                .executes(ChunkLimitCommand::listBlockLimits)))
                .then(Commands.literal("entity")
                        .then(Commands.literal("set")
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                BuiltInRegistries.ENTITY_TYPE.keySet().stream(),
                                                builder
                                        ))
                                        .then(Commands.argument("limit", IntegerArgumentType.integer(0))
                                                .executes(ChunkLimitCommand::setEntityLimit))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                ChunkLimitConfig.getEntityLimits().keySet().stream(),
                                                builder
                                        ))
                                        .executes(ChunkLimitCommand::removeEntityLimit)))
                        .then(Commands.literal("list")
                                .executes(ChunkLimitCommand::listEntityLimits)))
                .then(Commands.literal("check")
                        .executes(ChunkLimitCommand::checkCurrentChunk))
                .then(Commands.literal("dump")
                        .then(Commands.literal("entities")
                                .executes(context -> dumpEntities(context, false))
                                .then(Commands.literal("all")
                                        .executes(context -> dumpEntities(context, true)))));
    }

    private static boolean canManage(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.CHUNK_LIMIT);
        }
        return source.hasPermission(2);
    }

    private static int setEnabled(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(context, "value");
        if (!ChunkLimitConfig.setEnabled(enabled)) {
            throw SAVE_FAILED.create();
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENABLED_SET, enabled);
        return 1;
    }

    private static int setBlockLimit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation id = parseBlockId(context, "id");
        return applyBlockLimit(context, id, IntegerArgumentType.getInteger(context, "limit"));
    }

    private static int setHeldBlockLimit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        ResourceLocation id = blockIdFromHeldItem(player);
        return applyBlockLimit(context, id, IntegerArgumentType.getInteger(context, "limit"));
    }

    private static int applyBlockLimit(CommandContext<CommandSourceStack> context, ResourceLocation id, int limit)
            throws CommandSyntaxException {
        if (!ChunkLimitConfig.setBlockLimit(id, limit)) {
            throw SAVE_FAILED.create();
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_BLOCK_SET, id.toString(), limit);
        return 1;
    }

    private static int removeBlockLimit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation id = parseBlockId(context, "id");
        return applyBlockLimitRemoval(context, id);
    }

    private static int removeHeldBlockLimit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        ResourceLocation id = blockIdFromHeldItem(player);
        return applyBlockLimitRemoval(context, id);
    }

    private static int applyBlockLimitRemoval(CommandContext<CommandSourceStack> context, ResourceLocation id) {
        if (!ChunkLimitConfig.removeBlockLimit(id)) {
            sendFailure(context, CointCoreMessages.CHUNK_LIMIT_NOT_FOUND, id.toString());
            return 0;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_BLOCK_REMOVED, id.toString());
        return 1;
    }

    private static int listBlockLimits(CommandContext<CommandSourceStack> context) {
        Map<ResourceLocation, Integer> limits = ChunkLimitConfig.getBlockLimits();
        if (limits.isEmpty()) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_BLOCK_LIST_EMPTY);
            return 1;
        }
        for (Map.Entry<ResourceLocation, Integer> entry : limits.entrySet()) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_BLOCK_LIST_ENTRY, entry.getKey().toString(), entry.getValue());
        }
        return limits.size();
    }

    private static int setEntityLimit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation id = parseEntityId(context, "id");
        int limit = IntegerArgumentType.getInteger(context, "limit");
        if (!ChunkLimitConfig.setEntityLimit(id, limit)) {
            throw SAVE_FAILED.create();
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENTITY_SET, id.toString(), limit);
        return 1;
    }

    private static int removeEntityLimit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation id = parseEntityId(context, "id");
        if (!ChunkLimitConfig.removeEntityLimit(id)) {
            sendFailure(context, CointCoreMessages.CHUNK_LIMIT_NOT_FOUND, id.toString());
            return 0;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENTITY_REMOVED, id.toString());
        return 1;
    }

    private static int listEntityLimits(CommandContext<CommandSourceStack> context) {
        Map<ResourceLocation, Integer> limits = ChunkLimitConfig.getEntityLimits();
        if (limits.isEmpty()) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENTITY_LIST_EMPTY);
            return 1;
        }
        for (Map.Entry<ResourceLocation, Integer> entry : limits.entrySet()) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENTITY_LIST_ENTRY, entry.getKey().toString(), entry.getValue());
        }
        return limits.size();
    }

    private static int checkCurrentChunk(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level;
        ChunkPos chunkPos;

        if (source.getEntity() instanceof ServerPlayer player) {
            level = player.serverLevel();
            chunkPos = player.chunkPosition();
        } else {
            throw REQUIRES_PLAYER.create();
        }

        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_CHECK_HEADER, chunkPos.x, chunkPos.z);
        for (ChunkLimitService.ChunkLimitStatusEntry entry : ChunkLimitService.collectChunkStatus(level, chunkPos)) {
            if (entry.block()) {
                sendSuccess(
                        context,
                        CointCoreMessages.CHUNK_LIMIT_CHECK_BLOCK_ENTRY,
                        entry.id().toString(),
                        entry.current(),
                        entry.limit()
                );
            } else {
                sendSuccess(
                        context,
                        CointCoreMessages.CHUNK_LIMIT_CHECK_ENTITY_ENTRY,
                        entry.id().toString(),
                        entry.current(),
                        entry.limit()
                );
            }
        }
        return 1;
    }

    private static int dumpEntities(CommandContext<CommandSourceStack> context, boolean includeAllEntities)
            throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        try {
            EntityWorldDumpService.DumpResult result = EntityWorldDumpService.writeDump(server, includeAllEntities);
            sendSuccess(
                    context,
                    CointCoreMessages.CHUNK_LIMIT_DUMP_SUCCESS,
                    result.path().toString(),
                    result.entityTypes(),
                    result.totalEntities(),
                    result.filter()
            );
            return 1;
        } catch (IOException exception) {
            throw DUMP_FAILED.create();
        }
    }

    private static ResourceLocation parseBlockId(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
        ResourceLocation id = parseId(context, argument);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            throw UNKNOWN_BLOCK.create();
        }
        return id;
    }

    private static ResourceLocation parseEntityId(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
        ResourceLocation id = parseId(context, argument);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            throw UNKNOWN_ENTITY.create();
        }
        return id;
    }

    private static ResourceLocation parseId(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocation.tryParse(StringArgumentType.getString(context, argument).trim());
        if (id == null) {
            throw INVALID_ID.create();
        }
        return id;
    }

    private static ServerPlayer requirePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            return player;
        }
        throw REQUIRES_PLAYER.create();
    }

    private static ResourceLocation blockIdFromHeldItem(ServerPlayer player) throws CommandSyntaxException {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            throw NO_HELD_BLOCK.create();
        }

        Block block;
        if (held.getItem() instanceof BlockItem blockItem) {
            block = blockItem.getBlock();
        } else {
            block = Block.byItem(held.getItem());
        }

        if (block == null || block.defaultBlockState().isAir() || block == Blocks.AIR) {
            throw NO_HELD_BLOCK.create();
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) {
            throw UNKNOWN_BLOCK.create();
        }
        return id;
    }

    private static void sendSuccess(CommandContext<CommandSourceStack> context, String key, Object... args) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            source.sendSuccess(() -> CointCoreMessages.forPlayer(player, key, args), true);
        } else {
            source.sendSuccess(() -> CointCoreMessages.forConsole(key, args), true);
        }
    }

    private static void sendFailure(CommandContext<CommandSourceStack> context, String key, Object... args) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            source.sendFailure(CointCoreMessages.forPlayer(player, key, args));
        } else {
            source.sendFailure(CointCoreMessages.forConsole(key, args));
        }
    }
}
