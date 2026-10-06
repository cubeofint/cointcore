package com.mawlee.cointcore.ftbranks;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FtbRanksLuckPermsResolutionTest {
    private static final String COMMAND = "command.fly";
    private static final String HOMES = "ftbessentials.home.max";
    private static final String ULTIMINE = "ftbultimine.max_blocks";

    @Test
    void explicitFtbRanksBeatsLuckPerms() {
        FtbRanksLuckPermsResolution.Outcome outcome = FtbRanksLuckPermsResolution.resolve(
                true,
                true,
                true,
                COMMAND,
                query(Optional.of(true), Optional.of("ignored"))
        );
        assertEquals(FtbRanksLuckPermsResolution.Source.FTB_RANKS, outcome.source());
        assertTrue(outcome.luckPermsValue().isEmpty());
    }

    @Test
    void luckPermsCommandNodeWhenFtbRanksMissing() {
        FtbRanksLuckPermsResolution.Outcome outcome = FtbRanksLuckPermsResolution.resolve(
                true,
                true,
                false,
                COMMAND,
                query(Optional.of(true), Optional.empty())
        );
        assertEquals(FtbRanksLuckPermsResolution.Source.LUCKPERMS, outcome.source());
        FtbRanksLuckPermsResolution.ParsedValue.Bool value =
                assertInstanceOf(FtbRanksLuckPermsResolution.ParsedValue.Bool.class, outcome.luckPermsValue().orElseThrow());
        assertTrue(value.value());
    }

    @Test
    void luckPermsExplicitDenyForCommand() {
        FtbRanksLuckPermsResolution.Outcome outcome = FtbRanksLuckPermsResolution.resolve(
                true,
                true,
                false,
                "command.kick",
                query(Optional.of(false), Optional.empty())
        );
        FtbRanksLuckPermsResolution.ParsedValue.Bool value =
                assertInstanceOf(FtbRanksLuckPermsResolution.ParsedValue.Bool.class, outcome.luckPermsValue().orElseThrow());
        assertEquals(false, value.value());
    }

    @Test
    void essentialsMetaWhenFtbRanksMissing() {
        FtbRanksLuckPermsResolution.Outcome outcome = FtbRanksLuckPermsResolution.resolve(
                true,
                true,
                false,
                HOMES,
                query(Optional.empty(), Optional.of("5"))
        );
        assertEquals(FtbRanksLuckPermsResolution.Source.LUCKPERMS, outcome.source());
        FtbRanksLuckPermsResolution.ParsedValue.Num value =
                assertInstanceOf(FtbRanksLuckPermsResolution.ParsedValue.Num.class, outcome.luckPermsValue().orElseThrow());
        assertEquals(5, value.value().intValue());
    }

    @Test
    void ultimineMetaWhenFtbRanksMissing() {
        FtbRanksLuckPermsResolution.Outcome outcome = FtbRanksLuckPermsResolution.resolve(
                true,
                true,
                false,
                ULTIMINE,
                query(Optional.empty(), Optional.of("128"))
        );
        FtbRanksLuckPermsResolution.ParsedValue.Num value =
                assertInstanceOf(FtbRanksLuckPermsResolution.ParsedValue.Num.class, outcome.luckPermsValue().orElseThrow());
        assertEquals(128, value.value().intValue());
    }

    @Test
    void originalFallbackWhenLuckPermsHasNothing() {
        FtbRanksLuckPermsResolution.Outcome outcome = FtbRanksLuckPermsResolution.resolve(
                true,
                true,
                false,
                COMMAND,
                query(Optional.empty(), Optional.empty())
        );
        assertEquals(FtbRanksLuckPermsResolution.Source.FALLBACK, outcome.source());
    }

    @Test
    void disabledSwitchNeverAsksLuckPerms() {
        RecordingQuery query = new RecordingQuery(Optional.of(true), Optional.of("9"));
        FtbRanksLuckPermsResolution.Outcome outcome = FtbRanksLuckPermsResolution.resolve(
                false,
                true,
                false,
                COMMAND,
                query
        );
        assertEquals(FtbRanksLuckPermsResolution.Source.FALLBACK, outcome.source());
        assertEquals(0, query.calls);
    }

    @Test
    void absentLuckPermsUsesOriginalFallback() {
        FtbRanksLuckPermsResolution.Outcome outcome = FtbRanksLuckPermsResolution.resolve(
                true,
                false,
                false,
                HOMES,
                query(Optional.of(true), Optional.of("9"))
        );
        assertEquals(FtbRanksLuckPermsResolution.Source.FALLBACK, outcome.source());
        assertTrue(outcome.luckPermsValue().isEmpty());
    }

    @Test
    void commandPermissionBeatsCommandMeta() {
        Optional<FtbRanksLuckPermsResolution.ParsedValue> parsed = FtbRanksLuckPermsResolution.lookupLuckPerms(
                COMMAND,
                query(Optional.of(true), Optional.of("false"))
        );
        FtbRanksLuckPermsResolution.ParsedValue.Bool value =
                assertInstanceOf(FtbRanksLuckPermsResolution.ParsedValue.Bool.class, parsed.orElseThrow());
        assertTrue(value.value());
    }

    @Test
    void metaBeatsUnsetPermissionForNumericNodes() {
        Optional<FtbRanksLuckPermsResolution.ParsedValue> parsed = FtbRanksLuckPermsResolution.lookupLuckPerms(
                HOMES,
                query(Optional.of(true), Optional.of("3"))
        );
        FtbRanksLuckPermsResolution.ParsedValue.Num value =
                assertInstanceOf(FtbRanksLuckPermsResolution.ParsedValue.Num.class, parsed.orElseThrow());
        assertEquals(3, value.value().intValue());
    }

    @Test
    void parseMetaTypes() {
        assertInstanceOf(
                FtbRanksLuckPermsResolution.ParsedValue.Bool.class,
                FtbRanksLuckPermsResolution.parseMeta("true").orElseThrow()
        );
        assertInstanceOf(
                FtbRanksLuckPermsResolution.ParsedValue.Num.class,
                FtbRanksLuckPermsResolution.parseMeta("1.5").orElseThrow()
        );
        FtbRanksLuckPermsResolution.ParsedValue.Str quoted =
                assertInstanceOf(
                        FtbRanksLuckPermsResolution.ParsedValue.Str.class,
                        FtbRanksLuckPermsResolution.parseMeta("\"VIP {name}\"").orElseThrow()
                );
        assertEquals("VIP {name}", quoted.value());
        assertTrue(FtbRanksLuckPermsResolution.parseMeta("").isEmpty());
    }

    private static FtbRanksLuckPermsResolution.LuckPermsQuery query(
            Optional<Boolean> permission,
            Optional<String> meta
    ) {
        return new RecordingQuery(permission, meta);
    }

    private static final class RecordingQuery implements FtbRanksLuckPermsResolution.LuckPermsQuery {
        private final Optional<Boolean> permission;
        private final Optional<String> meta;
        private int calls;

        private RecordingQuery(Optional<Boolean> permission, Optional<String> meta) {
            this.permission = permission;
            this.meta = meta;
        }

        @Override
        public Optional<Boolean> permissionTristate(String node) {
            calls++;
            return permission;
        }

        @Override
        public Optional<String> meta(String node) {
            calls++;
            return meta;
        }
    }
}
