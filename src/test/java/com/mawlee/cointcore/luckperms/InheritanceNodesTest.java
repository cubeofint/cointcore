package com.mawlee.cointcore.luckperms;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InheritanceNodesTest {
    @Test
    void permissionKeyStartsWithGroupDot() {
        assertEquals("group.atm", InheritanceNodes.permissionKey("atm"));
        assertEquals("group.atm", InheritanceNodes.permissionKey(" ATM "));
        assertNull(InheritanceNodes.permissionKey(" "));
        assertNull(InheritanceNodes.permissionKey(null));
    }

    @Test
    void plainPermissionIsNotAnInheritanceKey() {
        assertTrue(InheritanceNodes.isInheritanceKey("group.atm"));
        assertTrue(InheritanceNodes.isInheritanceKey("GROUP.ATM"));
        assertFalse(InheritanceNodes.isInheritanceKey("atm"));
        assertFalse(InheritanceNodes.isInheritanceKey("group."));
        assertFalse(InheritanceNodes.isInheritanceKey(""));
        assertFalse(InheritanceNodes.isInheritanceKey(null));
    }

    @Test
    void groupsNotInheritedSkipsHeldAndDoesNotModifyWhenComplete() {
        assertEquals(List.of(), LuckPermsUsers.groupsNotInherited(List.of("atm", "titan"), List.of("ATM", "titan")));
        assertEquals(List.of("atm"), LuckPermsUsers.groupsNotInherited(List.of("atm", "titan"), List.of("titan")));
        assertEquals(List.of(), LuckPermsUsers.groupsNotInherited(List.of(), List.of("atm")));
    }
}
