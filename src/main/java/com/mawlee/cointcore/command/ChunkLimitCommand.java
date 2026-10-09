package com.mawlee.cointcore.command;

import com.mawlee.cointcore.chunklimit.ChunkLimitService;
import com.mawlee.cointcore.chunklimit.EntityWorldDumpService;
import com.mawlee.cointcore.chunklimit.MobLimitService;
import com.mawlee.cointcore.chunklimit.PlayerBlockLimitService;
import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.config.ChunkLimitConfig.LimitScope;
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
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class ChunkLimitCommand {
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
    private static final SimpleCommandExceptionType INVALID_NAMESPACE = new SimpleCommandExceptionType(
            Component.literal("Invalid mod namespace")
    );

    private ChunkLimitCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> chunkLimitCommand() {
        return Commands.literal("chunklimit")
                .requires(ChunkLimitCommand::canManage)
                .then(Commands.literal("enabled")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ChunkLimitCommand::setEnabled)))
                .then(blockBranch(LimitScope.CHUNK))
                .then(groupBranch(LimitScope.CHUNK))
                .then(entityBranch(LimitScope.CHUNK))
                .then(Commands.literal("check")
                        .executes(ChunkLimitCommand::checkCurrentChunk))
                .then(Commands.literal("team")
                        .then(blockBranch(LimitScope.TEAM))
                        .then(groupBranch(LimitScope.TEAM))
                        .then(entityBranch(LimitScope.TEAM))
                        .then(Commands.literal("check")
                                .executes(ChunkLimitCommand::checkCurrentTeam)))
                .then(Commands.literal("player")
                        .then(blockBranch(LimitScope.PLAYER))
                        .then(groupBranch(LimitScope.PLAYER))
                        .then(Commands.literal("check")
                                .executes(context -> checkPlayer(context, requirePlayer(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> checkPlayer(
                                                context,
                                                EntityArgument.getPlayer(context, "target")
                                        )))))
                .then(Commands.literal("dump")
                        .then(Commands.literal("entities")
                                .executes(context -> dumpEntities(context, false))
                                .then(Commands.literal("all")
                                        .executes(context -> dumpEntities(context, true)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> entityBranch(LimitScope scope) {
        return Commands.literal("entity")
                .then(Commands.literal("set")
                        .then(Commands.literal("cap")
                                .then(Commands.argument("limit", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setEntityCap(ctx, scope))))
                        .then(Commands.literal("mod")
                                .then(Commands.argument("modid", StringArgumentType.string())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                entityModNamespaces(),
                                                builder
                                        ))
                                        .then(Commands.argument("limit", IntegerArgumentType.integer(0))
                                                .executes(ctx -> setEntityModLimit(ctx, scope)))))
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        BuiltInRegistries.ENTITY_TYPE.keySet().stream(),
                                        builder
                                ))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setEntityLimit(ctx, scope)))))
                .then(Commands.literal("remove")
                        .then(Commands.literal("cap")
                                .executes(ctx -> removeEntityCap(ctx, scope)))
                        .then(Commands.literal("mod")
                                .then(Commands.argument("modid", StringArgumentType.string())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                configuredEntityModNamespaces(scope),
                                                builder
                                        ))
                                        .executes(ctx -> removeEntityModLimit(ctx, scope))))
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        exactEntityIds(scope).keySet().stream(),
                                        builder
                                ))
                                .executes(ctx -> removeEntityLimit(ctx, scope))))
                .then(Commands.literal("list")
                        .executes(ctx -> listEntityLimits(ctx, scope)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> blockBranch(LimitScope scope) {
        return Commands.literal("block")
                .then(Commands.literal("set")
                        .then(Commands.literal("held")
                                .then(Commands.argument("limit", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setHeldBlockLimit(ctx, scope))))
                        .then(Commands.literal("mod")
                                .then(Commands.argument("modid", StringArgumentType.string())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                modNamespaces(),
                                                builder
                                        ))
                                        .then(Commands.argument("limit", IntegerArgumentType.integer(0))
                                                .executes(ctx -> setModLimit(ctx, scope)))))
                        .then(Commands.literal("tag")
                                .then(Commands.argument("tagId", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                BuiltInRegistries.BLOCK.getTagNames().map(TagKey::location),
                                                builder
                                        ))
                                        .then(Commands.argument("limit", IntegerArgumentType.integer(0))
                                                .executes(ctx -> setTagLimit(ctx, scope)))))
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        BuiltInRegistries.BLOCK.keySet().stream(),
                                        builder
                                ))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setBlockLimit(ctx, scope)))))
                .then(Commands.literal("remove")
                        .then(Commands.literal("held")
                                .executes(ctx -> removeHeldBlockLimit(ctx, scope)))
                        .then(Commands.literal("mod")
                                .then(Commands.argument("modid", StringArgumentType.string())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                configuredModNamespaces(scope),
                                                builder
                                        ))
                                        .executes(ctx -> removeModLimit(ctx, scope))))
                        .then(Commands.literal("tag")
                                .then(Commands.argument("tagId", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                configuredTagIds(scope),
                                                builder
                                        ))
                                        .executes(ctx -> removeTagLimit(ctx, scope))))
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        exactBlockIds(scope).keySet().stream(),
                                        builder
                                ))
                                .executes(ctx -> removeBlockLimit(ctx, scope))))
                .then(Commands.literal("list")
                        .executes(ctx -> listBlockLimits(ctx, scope)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> groupBranch(LimitScope scope) {
        return Commands.literal("group")
                .then(Commands.literal("set")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("limit", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setGroupLimit(ctx, scope)))))
                .then(Commands.literal("add")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        groupNames(scope),
                                        builder
                                ))
                                .then(Commands.literal("held")
                                        .executes(ctx -> addHeldToGroup(ctx, scope)))
                                .then(Commands.argument("blockId", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                BuiltInRegistries.BLOCK.keySet().stream(),
                                                builder
                                        ))
                                        .executes(ctx -> addBlockToGroup(ctx, scope)))))
                .then(Commands.literal("remove-block")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        groupNames(scope),
                                        builder
                                ))
                                .then(Commands.literal("held")
                                        .executes(ctx -> removeHeldFromGroup(ctx, scope)))
                                .then(Commands.argument("blockId", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                groupBlockIds(scope, StringArgumentType.getString(context, "name")),
                                                builder
                                        ))
                                        .executes(ctx -> removeBlockFromGroup(ctx, scope)))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        groupNames(scope),
                                        builder
                                ))
                                .executes(ctx -> deleteGroup(ctx, scope))))
                .then(Commands.literal("list")
                        .executes(ctx -> listGroups(ctx, scope)));
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

    private static int setBlockLimit(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        ResourceLocation id = parseBlockId(context, "id");
        return applyBlockLimit(context, scope, id, IntegerArgumentType.getInteger(context, "limit"));
    }

    private static int setHeldBlockLimit(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        ResourceLocation id = blockIdFromHeldItem(player);
        return applyBlockLimit(context, scope, id, IntegerArgumentType.getInteger(context, "limit"));
    }

    private static int applyBlockLimit(
            CommandContext<CommandSourceStack> context,
            LimitScope scope,
            ResourceLocation id,
            int limit
    ) throws CommandSyntaxException {
        if (!ChunkLimitConfig.setBlockLimit(scope, id, limit)) {
            throw SAVE_FAILED.create();
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_BLOCK_SET, scopeLabel(scope) + id, limit);
        return 1;
    }

    private static int removeBlockLimit(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        ResourceLocation id = parseBlockId(context, "id");
        return applyBlockLimitRemoval(context, scope, id);
    }

    private static int removeHeldBlockLimit(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        ResourceLocation id = blockIdFromHeldItem(player);
        return applyBlockLimitRemoval(context, scope, id);
    }

    private static int applyBlockLimitRemoval(
            CommandContext<CommandSourceStack> context,
            LimitScope scope,
            ResourceLocation id
    ) {
        if (!ChunkLimitConfig.removeBlockLimit(scope, id)) {
            sendFailure(context, CointCoreMessages.CHUNK_LIMIT_NOT_FOUND, scopeLabel(scope) + id);
            return 0;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_BLOCK_REMOVED, scopeLabel(scope) + id);
        return 1;
    }

    private static int setModLimit(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        String modid = StringArgumentType.getString(context, "modid").trim();
        int limit = IntegerArgumentType.getInteger(context, "limit");
        if (!ChunkLimitConfig.setModLimit(scope, modid, limit)) {
            throw INVALID_NAMESPACE.create();
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_MOD_SET, scopeLabel(scope) + modid + ":*", limit);
        return 1;
    }

    private static int removeModLimit(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        String modid = StringArgumentType.getString(context, "modid").trim();
        if (!ChunkLimitConfig.removeModLimit(scope, modid)) {
            sendFailure(context, CointCoreMessages.CHUNK_LIMIT_NOT_FOUND, scopeLabel(scope) + modid + ":*");
            return 0;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_MOD_REMOVED, scopeLabel(scope) + modid + ":*");
        return 1;
    }

    private static int setTagLimit(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        ResourceLocation tagId = parseId(context, "tagId");
        int limit = IntegerArgumentType.getInteger(context, "limit");
        if (!ChunkLimitConfig.setTagLimit(scope, tagId, limit)) {
            throw SAVE_FAILED.create();
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_TAG_SET, scopeLabel(scope) + "#" + tagId, limit);
        return 1;
    }

    private static int removeTagLimit(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        ResourceLocation tagId = parseId(context, "tagId");
        if (!ChunkLimitConfig.removeTagLimit(scope, tagId)) {
            sendFailure(context, CointCoreMessages.CHUNK_LIMIT_NOT_FOUND, scopeLabel(scope) + "#" + tagId);
            return 0;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_TAG_REMOVED, scopeLabel(scope) + "#" + tagId);
        return 1;
    }

    private static int listBlockLimits(CommandContext<CommandSourceStack> context, LimitScope scope) {
        Map<ResourceLocation, Integer> limits = ChunkLimitConfig.getBlockLimits(scope);
        var tags = ChunkLimitConfig.getTagBindings(scope);
        var mods = ChunkLimitConfig.getModBindings(scope);
        var groups = ChunkLimitConfig.getGroups(scope);

        if (limits.isEmpty() && tags.isEmpty() && mods.isEmpty() && groups.isEmpty()) {
            sendSuccess(context, switch (scope) {
                case CHUNK -> CointCoreMessages.CHUNK_LIMIT_BLOCK_LIST_EMPTY;
                case TEAM -> CointCoreMessages.CHUNK_LIMIT_TEAM_LIST_EMPTY;
                case PLAYER -> CointCoreMessages.CHUNK_LIMIT_PLAYER_LIST_EMPTY;
            });
            return 1;
        }
        int count = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : limits.entrySet()) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_BLOCK_LIST_ENTRY, entry.getKey().toString(), entry.getValue());
            count++;
        }
        for (var tag : tags) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_BLOCK_LIST_ENTRY, tag.key().id(), tag.key().limit());
            count++;
        }
        for (var mod : mods) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_BLOCK_LIST_ENTRY, mod.key().id(), mod.key().limit());
            count++;
        }
        for (var group : groups.values()) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_BLOCK_LIST_ENTRY, "group:" + group.name(), group.limit());
            count++;
        }
        return count;
    }

    private static int setGroupLimit(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        int limit = IntegerArgumentType.getInteger(context, "limit");
        if (!ChunkLimitConfig.setGroupLimit(scope, name, limit)) {
            throw SAVE_FAILED.create();
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_GROUP_SET, scopeLabel(scope) + name, limit);
        return 1;
    }

    private static int addBlockToGroup(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        ResourceLocation id = parseBlockId(context, "blockId");
        if (!ChunkLimitConfig.addBlockToGroup(scope, name, id)) {
            sendFailure(context, CointCoreMessages.CHUNK_LIMIT_NOT_FOUND, scopeLabel(scope) + name);
            return 0;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_GROUP_ADD, scopeLabel(scope) + name, id.toString());
        return 1;
    }

    private static int addHeldToGroup(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        ResourceLocation id = blockIdFromHeldItem(requirePlayer(context));
        if (!ChunkLimitConfig.addBlockToGroup(scope, name, id)) {
            sendFailure(context, CointCoreMessages.CHUNK_LIMIT_NOT_FOUND, scopeLabel(scope) + name);
            return 0;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_GROUP_ADD, scopeLabel(scope) + name, id.toString());
        return 1;
    }

    private static int removeBlockFromGroup(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        ResourceLocation id = parseBlockId(context, "blockId");
        if (!ChunkLimitConfig.removeBlockFromGroup(scope, name, id)) {
            sendFailure(context, CointCoreMessages.CHUNK_LIMIT_NOT_FOUND, scopeLabel(scope) + name + "/" + id);
            return 0;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_GROUP_REMOVE_BLOCK, scopeLabel(scope) + name, id.toString());
        return 1;
    }

    private static int removeHeldFromGroup(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        ResourceLocation id = blockIdFromHeldItem(requirePlayer(context));
        if (!ChunkLimitConfig.removeBlockFromGroup(scope, name, id)) {
            sendFailure(context, CointCoreMessages.CHUNK_LIMIT_NOT_FOUND, scopeLabel(scope) + name + "/" + id);
            return 0;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_GROUP_REMOVE_BLOCK, scopeLabel(scope) + name, id.toString());
        return 1;
    }

    private static int deleteGroup(CommandContext<CommandSourceStack> context, LimitScope scope) {
        String name = StringArgumentType.getString(context, "name");
        if (!ChunkLimitConfig.deleteGroup(scope, name)) {
            sendFailure(context, CointCoreMessages.CHUNK_LIMIT_NOT_FOUND, scopeLabel(scope) + name);
            return 0;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_GROUP_DELETED, scopeLabel(scope) + name);
        return 1;
    }

    private static int listGroups(CommandContext<CommandSourceStack> context, LimitScope scope) {
        Map<String, ChunkLimitConfig.GroupLimit> groups = ChunkLimitConfig.getGroups(scope);
        if (groups.isEmpty()) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_GROUP_LIST_EMPTY);
            return 1;
        }
        for (ChunkLimitConfig.GroupLimit group : groups.values()) {
            sendSuccess(
                    context,
                    CointCoreMessages.CHUNK_LIMIT_GROUP_LIST_ENTRY,
                    group.name(),
                    group.limit(),
                    group.blocks().size()
            );
        }
        return groups.size();
    }

    private static int setEntityLimit(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        ResourceLocation id = parseEntityId(context, "id");
        int limit = IntegerArgumentType.getInteger(context, "limit");
        if (!ChunkLimitConfig.setEntityLimit(scope, id, limit)) {
            throw SAVE_FAILED.create();
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENTITY_SET, scopeLabel(scope) + id, limit);
        return 1;
    }

    private static int removeEntityLimit(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        ResourceLocation id = parseEntityId(context, "id");
        if (!ChunkLimitConfig.removeEntityLimit(scope, id)) {
            sendFailure(context, CointCoreMessages.CHUNK_LIMIT_NOT_FOUND, scopeLabel(scope) + id);
            return 0;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENTITY_REMOVED, scopeLabel(scope) + id);
        return 1;
    }

    private static int setEntityModLimit(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        String modid = StringArgumentType.getString(context, "modid").trim();
        int limit = IntegerArgumentType.getInteger(context, "limit");
        if (!ChunkLimitConfig.setEntityModLimit(scope, modid, limit)) {
            throw INVALID_NAMESPACE.create();
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENTITY_MOD_SET, scopeLabel(scope) + modid + ":*", limit);
        return 1;
    }

    private static int removeEntityModLimit(CommandContext<CommandSourceStack> context, LimitScope scope) {
        String modid = StringArgumentType.getString(context, "modid").trim();
        if (!ChunkLimitConfig.removeEntityModLimit(scope, modid)) {
            sendFailure(context, CointCoreMessages.CHUNK_LIMIT_NOT_FOUND, scopeLabel(scope) + modid + ":*");
            return 0;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENTITY_MOD_REMOVED, scopeLabel(scope) + modid + ":*");
        return 1;
    }

    private static int setEntityCap(CommandContext<CommandSourceStack> context, LimitScope scope)
            throws CommandSyntaxException {
        int limit = IntegerArgumentType.getInteger(context, "limit");
        if (!ChunkLimitConfig.setEntityCap(scope, limit)) {
            throw SAVE_FAILED.create();
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENTITY_CAP_SET, scopeLabel(scope) + "*", limit);
        return 1;
    }

    private static int removeEntityCap(CommandContext<CommandSourceStack> context, LimitScope scope) {
        if (!ChunkLimitConfig.removeEntityCap(scope)) {
            sendFailure(context, CointCoreMessages.CHUNK_LIMIT_NOT_FOUND, scopeLabel(scope) + "*");
            return 0;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENTITY_CAP_REMOVED, scopeLabel(scope) + "*");
        return 1;
    }

    private static int listEntityLimits(CommandContext<CommandSourceStack> context, LimitScope scope) {
        Map<ResourceLocation, Integer> limits = exactEntityIds(scope);
        var mods = scope == LimitScope.TEAM
                ? ChunkLimitConfig.getTeamEntityModBindings()
                : ChunkLimitConfig.getEntityModBindings();
        Integer cap = ChunkLimitConfig.getEntityCap(scope);
        if (limits.isEmpty() && mods.isEmpty() && cap == null) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENTITY_LIST_EMPTY);
            return 1;
        }
        int count = 0;
        if (cap != null) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENTITY_LIST_ENTRY, "*(mob cap)", cap);
            count++;
        }
        for (Map.Entry<ResourceLocation, Integer> entry : limits.entrySet()) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENTITY_LIST_ENTRY, entry.getKey().toString(), entry.getValue());
            count++;
        }
        for (var mod : mods) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_ENTITY_LIST_ENTRY, mod.key().id(), mod.key().limit());
            count++;
        }
        return count;
    }

    private static int checkCurrentChunk(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        ServerLevel level = player.serverLevel();
        ChunkPos chunkPos = player.chunkPosition();

        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_CHECK_HEADER, chunkPos.x, chunkPos.z);
        for (ChunkLimitService.ChunkLimitStatusEntry entry : ChunkLimitService.collectChunkStatus(level, chunkPos)) {
            if (entry.block()) {
                sendSuccess(
                        context,
                        CointCoreMessages.CHUNK_LIMIT_CHECK_BLOCK_ENTRY,
                        entry.id(),
                        entry.current(),
                        entry.limit(),
                        entry.inert()
                );
            } else {
                sendSuccess(
                        context,
                        CointCoreMessages.CHUNK_LIMIT_CHECK_ENTITY_ENTRY,
                        entry.id(),
                        entry.current(),
                        entry.limit()
                );
            }
        }
        return 1;
    }

    private static int checkCurrentTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        ServerLevel level = player.serverLevel();
        ChunkPos chunkPos = player.chunkPosition();
        var entries = ChunkLimitService.collectTeamStatus(level, chunkPos);
        var mobEntries = MobLimitService.collectTeamMobStatus(level, chunkPos);
        if (entries.isEmpty() && mobEntries.isEmpty()) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_TEAM_CHECK_EMPTY);
            return 1;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_TEAM_CHECK_HEADER, chunkPos.x, chunkPos.z);
        for (ChunkLimitService.ChunkLimitStatusEntry entry : entries) {
            sendSuccess(
                    context,
                    CointCoreMessages.CHUNK_LIMIT_CHECK_BLOCK_ENTRY,
                    entry.id(),
                    entry.current(),
                    entry.limit(),
                    entry.inert()
            );
        }
        for (ChunkLimitService.ChunkLimitStatusEntry entry : mobEntries) {
            sendSuccess(
                    context,
                    CointCoreMessages.CHUNK_LIMIT_CHECK_ENTITY_ENTRY,
                    entry.id(),
                    entry.current(),
                    entry.limit()
            );
        }
        return 1;
    }

    private static int checkPlayer(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        var entries = PlayerBlockLimitService.collectStatus(context.getSource().getServer(), target.getUUID());
        if (entries.isEmpty()) {
            sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_PLAYER_CHECK_EMPTY);
            return 1;
        }
        sendSuccess(context, CointCoreMessages.CHUNK_LIMIT_PLAYER_CHECK_HEADER, target.getGameProfile().getName());
        for (ChunkLimitService.ChunkLimitStatusEntry entry : entries) {
            sendSuccess(
                    context,
                    CointCoreMessages.CHUNK_LIMIT_CHECK_ENTITY_ENTRY,
                    entry.id(),
                    entry.current(),
                    entry.limit()
            );
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

    private static String scopeLabel(LimitScope scope) {
        return switch (scope) {
            case CHUNK -> "";
            case TEAM -> "team/";
            case PLAYER -> "player/";
        };
    }

    private static Map<ResourceLocation, Integer> exactBlockIds(LimitScope scope) {
        return ChunkLimitConfig.getBlockLimits(scope);
    }

    private static Map<ResourceLocation, Integer> exactEntityIds(LimitScope scope) {
        return scope == LimitScope.TEAM
                ? ChunkLimitConfig.getTeamEntityLimits()
                : ChunkLimitConfig.getEntityLimits();
    }

    private static Iterable<String> entityModNamespaces() {
        Set<String> namespaces = new LinkedHashSet<>();
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            namespaces.add(id.getNamespace());
        }
        return namespaces;
    }

    private static Iterable<String> configuredEntityModNamespaces(LimitScope scope) {
        var bindings = scope == LimitScope.TEAM
                ? ChunkLimitConfig.getTeamEntityModBindings()
                : ChunkLimitConfig.getEntityModBindings();
        Set<String> names = new LinkedHashSet<>();
        for (var binding : bindings) {
            names.add(binding.namespace());
        }
        return names;
    }

    private static Iterable<String> groupNames(LimitScope scope) {
        return ChunkLimitConfig.getGroups(scope).keySet();
    }

    private static Stream<ResourceLocation> groupBlockIds(LimitScope scope, String name) {
        ChunkLimitConfig.GroupLimit group = ChunkLimitConfig.getGroups(scope).get(name);
        return group == null ? Stream.empty() : group.blocks().stream();
    }

    private static Iterable<String> configuredModNamespaces(LimitScope scope) {
        var bindings = ChunkLimitConfig.getModBindings(scope);
        Set<String> names = new LinkedHashSet<>();
        for (var binding : bindings) {
            names.add(binding.namespace());
        }
        return names;
    }

    private static Iterable<String> configuredTagIds(LimitScope scope) {
        var bindings = ChunkLimitConfig.getTagBindings(scope);
        Set<String> names = new LinkedHashSet<>();
        for (var binding : bindings) {
            names.add(binding.tag().location().toString());
        }
        return names;
    }

    private static Iterable<String> modNamespaces() {
        Set<String> namespaces = new LinkedHashSet<>();
        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
            namespaces.add(id.getNamespace());
        }
        return namespaces;
    }

    private static ResourceLocation parseBlockId(CommandContext<CommandSourceStack> context, String argument)
            throws CommandSyntaxException {
        ResourceLocation id = parseId(context, argument);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            throw UNKNOWN_BLOCK.create();
        }
        return id;
    }

    private static ResourceLocation parseEntityId(CommandContext<CommandSourceStack> context, String argument)
            throws CommandSyntaxException {
        ResourceLocation id = parseId(context, argument);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            throw UNKNOWN_ENTITY.create();
        }
        return id;
    }

    private static ResourceLocation parseId(CommandContext<CommandSourceStack> context, String argument) {
        return ResourceLocationArgument.getId(context, argument);
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
