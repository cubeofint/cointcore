package com.mawlee.cointcore.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Map;

public final class CommandRegistration {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("rawtypes")
    private static final Field CHILDREN_FIELD = findField("children");

    @SuppressWarnings("rawtypes")
    private static final Field LITERALS_FIELD = findField("literals");

    @SuppressWarnings("rawtypes")
    private static final Field ARGUMENTS_FIELD = findField("arguments");

    private CommandRegistration() {
    }

    public static void unregisterRootLiteral(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot();
        Map<String, CommandNode<CommandSourceStack>> children = readMap(root, CHILDREN_FIELD);
        Map<String, LiteralCommandNode<CommandSourceStack>> literals = readMap(root, LITERALS_FIELD);
        Map<String, ArgumentCommandNode<CommandSourceStack, ?>> arguments = readMap(root, ARGUMENTS_FIELD);
        if (children == null || literals == null || arguments == null) {
            return;
        }

        children.remove(name);
        literals.remove(name);
        arguments.remove(name);
    }

    public static void replaceRootLiteral(
            CommandDispatcher<CommandSourceStack> dispatcher,
            String name,
            com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> command
    ) {
        unregisterRootLiteral(dispatcher, name);
        dispatcher.register(command);
    }

    private static Field findField(String name) {
        try {
            Field field = CommandNode.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Failed to access Brigadier CommandNode field '{}'", name, exception);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<String, T> readMap(CommandNode<CommandSourceStack> node, Field field) {
        if (field == null) {
            return null;
        }

        try {
            return (Map<String, T>) field.get(node);
        } catch (IllegalAccessException exception) {
            LOGGER.error("Failed to read Brigadier CommandNode field '{}'", field.getName(), exception);
            return null;
        }
    }
}
