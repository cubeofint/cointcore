package com.mawlee.cointcore.command;

import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.function.Predicate;

/**
 * Opens vanilla {@code /tp} / {@code /teleport} to holders of {@code cointcore.tp.self} (donor rank), limited to
 * moving themselves: {@code /tp <x y z>} and {@code /tp <player>}. The {@code <targets> ...} branch (moving other
 * entities, facing, rotation) keeps the vanilla op requirement. Entity selectors still need op level 2, so a
 * donor can only type a player name.
 */
public final class SelfTeleportCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String OTHERS_BRANCH = "targets";

    private SelfTeleportCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> teleport = dispatcher.getRoot().getChild("teleport");
        if (teleport == null) {
            LOGGER.warn("Vanilla /teleport not found; cointcore.tp.self has no effect");
            return;
        }
        if (teleport.getRequirement() instanceof OrSelf) {
            return; // already applied (ModCommands runs again on server start)
        }
        Predicate<CommandSourceStack> vanilla = teleport.getRequirement();
        CommandRegistration.setRequirement(teleport, new OrSelf(vanilla));
        CommandRegistration.setRequirement(teleport.getChild(OTHERS_BRANCH), vanilla);
        CommandNode<CommandSourceStack> tp = dispatcher.getRoot().getChild("tp");
        if (tp != null && tp.getRedirect() == teleport) {
            CommandRegistration.setRequirement(tp, new OrSelf(tp.getRequirement()));
        }
    }

    /** Vanilla requirement or {@code cointcore.tp.self}; a named type so a second {@link #apply} can detect it. */
    private record OrSelf(Predicate<CommandSourceStack> vanilla) implements Predicate<CommandSourceStack> {
        @Override
        public boolean test(CommandSourceStack source) {
            return vanilla.test(source)
                    || source.getEntity() instanceof ServerPlayer player
                    && PermissionService.has(player, CointPermissionNodes.TP_SELF);
        }
    }
}
