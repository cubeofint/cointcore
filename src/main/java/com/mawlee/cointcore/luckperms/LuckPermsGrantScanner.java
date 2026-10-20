package com.mawlee.cointcore.luckperms;

import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.query.QueryOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The only code that links against LuckPerms API types. Kept out of {@link LuckPermsIntegration} so that class
 * still loads (and {@code isAvailable()} answers false) where LuckPerms is not installed, e.g. single player.
 * Only call after {@link LuckPermsIntegration#isAvailable()}.
 */
final class LuckPermsGrantScanner {
    private LuckPermsGrantScanner() {
    }

    /**
     * Own user nodes plus each inherited group's own nodes. Avoids
     * {@code resolveDistinctInheritedNodes} (copy + sort of the full node set).
     */
    static TemporaryGrantSnapshot scanTemporaryGrants(Object holder) {
        if (!(holder instanceof User user)) {
            return TemporaryGrantSnapshot.EMPTY;
        }
        List<TemporaryGrantSnapshot.Grant> grants = new ArrayList<>();
        collectTemporaryGrants(user, grants);
        QueryOptions queryOptions = user.getQueryOptions();
        if (queryOptions != null) {
            Collection<Group> groups = user.getInheritedGroups(queryOptions);
            if (groups != null) {
                for (Group group : groups) {
                    collectTemporaryGrants(group, grants);
                }
            }
        }
        return TemporaryGrantSnapshot.collect(grants);
    }

    private static void collectTemporaryGrants(PermissionHolder holder, List<TemporaryGrantSnapshot.Grant> grants) {
        if (holder == null) {
            return;
        }
        Collection<Node> nodes = holder.getNodes();
        if (nodes == null) {
            return;
        }
        for (Node node : nodes) {
            if (node == null || !node.hasExpiry() || !node.getValue()) {
                continue;
            }
            NodeType<?> type = node.getType();
            boolean permission = NodeType.PERMISSION.equals(type);
            boolean inheritance = NodeType.INHERITANCE.equals(type);
            if (!permission && !inheritance) {
                continue;
            }
            grants.add(new TemporaryGrantSnapshot.Grant(
                    permission ? node.getKey() : null,
                    inheritance,
                    node.hasExpired(),
                    node.getExpiry()
            ));
        }
    }
}
