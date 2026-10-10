package com.mawlee.cointcore.lang;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mawlee.cointcore.CointCore;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CointCoreMessages {
    private static final Gson GSON = new Gson();
    /** Avoid anonymous TypeToken subclass ($1) — breaks hot-swapped jars on live JVMs. */
    private static final Type LANG_MAP_TYPE = TypeToken.getParameterized(Map.class, String.class, String.class).getType();
    private static final String DEFAULT_LANG = "en_us";
    private static final Map<String, Map<String, String>> TRANSLATIONS = loadTranslations();

    public static final String VANISH_ENABLED = "message.cointcore.vanish.enabled";
    public static final String VANISH_DISABLED = "message.cointcore.vanish.disabled";
    public static final String VANISH_MOBS_ENABLED = "message.cointcore.vanish.mobs.enabled";
    public static final String VANISH_MOBS_DISABLED = "message.cointcore.vanish.mobs.disabled";
    public static final String CONFIG_RELOAD_SUCCESS = "message.cointcore.config.reload.success";
    public static final String CONFIG_RELOAD_FAILED = "message.cointcore.config.reload.failed";
    public static final String NIGHT_VISION_ENABLED = "message.cointcore.nv.enabled";
    public static final String NIGHT_VISION_DISABLED = "message.cointcore.nv.disabled";
    public static final String FLY_DISABLED = "message.cointcore.fly.disabled";
    public static final String GOD_DISABLED = "message.cointcore.god.disabled";
    public static final String MSG_SENT = "message.cointcore.msg.sent";
    public static final String MSG_RECEIVED = "message.cointcore.msg.received";
    public static final String MSG_PLAYER_NOT_FOUND = "message.cointcore.msg.player_not_found";
    public static final String MSG_PLAYER_OFFLINE = "message.cointcore.msg.player_offline";
    public static final String MSG_CANNOT_MESSAGE_SELF = "message.cointcore.msg.cannot_message_self";
    public static final String MSG_NO_REPLY_TARGET = "message.cointcore.msg.no_reply_target";
    public static final String IGNORE_ADDED = "message.cointcore.ignore.added";
    public static final String IGNORE_REMOVED = "message.cointcore.ignore.removed";
    public static final String IGNORE_PLAYER_NOT_FOUND = "message.cointcore.ignore.player_not_found";
    public static final String IGNORE_CANNOT_SELF = "message.cointcore.ignore.cannot_self";
    public static final String MUTE_APPLIED = "message.cointcore.mute.applied";
    public static final String MUTE_APPLIED_PERM = "message.cointcore.mute.applied.perm";
    public static final String MUTE_RECEIVED = "message.cointcore.mute.received";
    public static final String MUTE_RECEIVED_PERM = "message.cointcore.mute.received.perm";
    public static final String MUTE_BLOCKED = "message.cointcore.mute.blocked";
    public static final String MUTE_LIFTED = "message.cointcore.mute.lifted";
    public static final String MUTE_NOT_MUTED = "message.cointcore.mute.not_muted";
    public static final String MUTE_PLAYER_NOT_FOUND = "message.cointcore.mute.player_not_found";
    public static final String UNMUTE_APPLIED = "message.cointcore.unmute.applied";
    public static final String WORLD_CLEANUP_CLEARED = "message.cointcore.world_cleanup.cleared";
    public static final String WORLD_CLEANUP_PREVENTED = "message.cointcore.world_cleanup.prevented";
    public static final String WORLD_CLEANUP_MANUAL_STATUS = "message.cointcore.world_cleanup.manual_status";
    public static final String WORLD_CLEANUP_DISABLED = "message.cointcore.world_cleanup.disabled";
    public static final String CHUNK_LIMIT_BLOCK_DENIED = "message.cointcore.chunklimit.block_denied";
    public static final String CHUNK_LIMIT_ENTITY_DENIED = "message.cointcore.chunklimit.entity_denied";
    public static final String CHUNK_LIMIT_ENABLED_SET = "message.cointcore.chunklimit.enabled_set";
    public static final String CHUNK_LIMIT_BLOCK_SET = "message.cointcore.chunklimit.block_set";
    public static final String CHUNK_LIMIT_BLOCK_REMOVED = "message.cointcore.chunklimit.block_removed";
    public static final String CHUNK_LIMIT_BLOCK_LIST_EMPTY = "message.cointcore.chunklimit.block_list_empty";
    public static final String CHUNK_LIMIT_BLOCK_LIST_ENTRY = "message.cointcore.chunklimit.block_list_entry";
    public static final String CHUNK_LIMIT_ENTITY_SET = "message.cointcore.chunklimit.entity_set";
    public static final String CHUNK_LIMIT_ENTITY_REMOVED = "message.cointcore.chunklimit.entity_removed";
    public static final String CHUNK_LIMIT_ENTITY_LIST_EMPTY = "message.cointcore.chunklimit.entity_list_empty";
    public static final String CHUNK_LIMIT_ENTITY_LIST_ENTRY = "message.cointcore.chunklimit.entity_list_entry";
    public static final String CHUNK_LIMIT_ENTITY_MOD_SET = "message.cointcore.chunklimit.entity_mod_set";
    public static final String CHUNK_LIMIT_ENTITY_MOD_REMOVED = "message.cointcore.chunklimit.entity_mod_removed";
    public static final String CHUNK_LIMIT_ENTITY_CAP_SET = "message.cointcore.chunklimit.entity_cap_set";
    public static final String CHUNK_LIMIT_ENTITY_CAP_REMOVED = "message.cointcore.chunklimit.entity_cap_removed";
    public static final String CHUNK_LIMIT_NOT_FOUND = "message.cointcore.chunklimit.not_found";
    public static final String CHUNK_LIMIT_CHECK_HEADER = "message.cointcore.chunklimit.check_header";
    public static final String CHUNK_LIMIT_CHECK_BLOCK_ENTRY = "message.cointcore.chunklimit.check_block_entry";
    public static final String CHUNK_LIMIT_CHECK_ENTITY_ENTRY = "message.cointcore.chunklimit.check_entity_entry";
    public static final String CHUNK_LIMIT_DUMP_SUCCESS = "message.cointcore.chunklimit.dump_success";
    public static final String CHUNK_LIMIT_TEAM_BLOCK_DENIED = "message.cointcore.chunklimit.team.block_denied";
    public static final String CHUNK_LIMIT_MOD_SET = "message.cointcore.chunklimit.mod_set";
    public static final String CHUNK_LIMIT_MOD_REMOVED = "message.cointcore.chunklimit.mod_removed";
    public static final String CHUNK_LIMIT_TAG_SET = "message.cointcore.chunklimit.tag_set";
    public static final String CHUNK_LIMIT_TAG_REMOVED = "message.cointcore.chunklimit.tag_removed";
    public static final String CHUNK_LIMIT_GROUP_SET = "message.cointcore.chunklimit.group_set";
    public static final String CHUNK_LIMIT_GROUP_DELETED = "message.cointcore.chunklimit.group_deleted";
    public static final String CHUNK_LIMIT_GROUP_ADD = "message.cointcore.chunklimit.group_add";
    public static final String CHUNK_LIMIT_GROUP_REMOVE_BLOCK = "message.cointcore.chunklimit.group_remove_block";
    public static final String CHUNK_LIMIT_GROUP_LIST_EMPTY = "message.cointcore.chunklimit.group_list_empty";
    public static final String CHUNK_LIMIT_GROUP_LIST_ENTRY = "message.cointcore.chunklimit.group_list_entry";
    public static final String CHUNK_LIMIT_TEAM_CHECK_HEADER = "message.cointcore.chunklimit.team.check_header";
    public static final String CHUNK_LIMIT_TEAM_CHECK_EMPTY = "message.cointcore.chunklimit.team.check_empty";
    public static final String CHUNK_LIMIT_TEAM_LIST_EMPTY = "message.cointcore.chunklimit.team.list_empty";
    public static final String CHUNK_LIMIT_PLAYER_BLOCK_DENIED = "message.cointcore.chunklimit.player.block_denied";
    public static final String CHUNK_LIMIT_PLAYER_CHECK_HEADER = "message.cointcore.chunklimit.player.check_header";
    public static final String CHUNK_LIMIT_PLAYER_CHECK_EMPTY = "message.cointcore.chunklimit.player.check_empty";
    public static final String CHUNK_LIMIT_PLAYER_LIST_EMPTY = "message.cointcore.chunklimit.player.list_empty";
    /** @deprecated Use {@link #WORLD_CLEANUP_CLEARED} */
    @Deprecated
    public static final String CRASH_UTILITIES_CLEAR_RESULT = WORLD_CLEANUP_CLEARED;
    public static final String BAN_APPLIED = "message.cointcore.ban.applied";
    public static final String BAN_APPLIED_PERM = "message.cointcore.ban.applied.perm";
    public static final String BAN_RECEIVED = "message.cointcore.ban.received";
    public static final String BAN_RECEIVED_PERM = "message.cointcore.ban.received.perm";
    public static final String BAN_ALREADY_BANNED = "message.cointcore.ban.already_banned";
    public static final String BAN_PLAYER_NOT_FOUND = "message.cointcore.ban.player_not_found";
    public static final String TPL_SUCCESS_ONLINE = "message.cointcore.tpl.success.online";
    public static final String TPL_SUCCESS_OFFLINE = "message.cointcore.tpl.success.offline";
    public static final String TPL_PLAYER_NOT_FOUND = "message.cointcore.tpl.player_not_found";
    public static final String TPL_NO_PLAYER_DATA = "message.cointcore.tpl.no_player_data";
    public static final String TPL_DIMENSION_UNAVAILABLE = "message.cointcore.tpl.dimension_unavailable";
    public static final String TPL_EXECUTOR_MUST_BE_PLAYER = "message.cointcore.tpl.executor_must_be_player";
    public static final String TPL_CANNOT_SELF = "message.cointcore.tpl.cannot_self";
    public static final String WARN_APPLIED = "message.cointcore.warn.applied";
    public static final String WARN_RECEIVED = "message.cointcore.warn.received";
    public static final String WARN_PLAYER_NOT_FOUND = "message.cointcore.warn.player_not_found";
    public static final String PUNISHMENTS_HEADER = "message.cointcore.punishments.header";
    public static final String PUNISHMENTS_EMPTY = "message.cointcore.punishments.empty";
    public static final String PUNISHMENTS_ENTRY = "message.cointcore.punishments.entry";
    public static final String PUNISHMENTS_PLAYER_NOT_FOUND = "message.cointcore.punishments.player_not_found";
    public static final String PUNISHMENT_TYPE_WARN = "message.cointcore.punishment.type.warn";
    public static final String PUNISHMENT_TYPE_MUTE = "message.cointcore.punishment.type.mute";
    public static final String PUNISHMENT_TYPE_UNMUTE = "message.cointcore.punishment.type.unmute";
    public static final String PUNISHMENT_TYPE_BAN = "message.cointcore.punishment.type.ban";
    public static final String PUNISHMENT_TYPE_UNBAN = "message.cointcore.punishment.type.unban";
    public static final String PUNISHMENT_DURATION_NA = "message.cointcore.punishment.duration.na";
    public static final String PUNISHMENT_DURATION_PERM = "message.cointcore.punishment.duration.perm";
    public static final String PVP_ENABLED = "message.cointcore.pvp.enabled";
    public static final String PVP_DISABLED = "message.cointcore.pvp.disabled";
    public static final String PVP_PEACE_REQUIRED = "message.cointcore.pvp.peace_required";
    public static final String KEEP_INVENTORY_RESTORED = "message.cointcore.keep_inventory.restored";
    public static final String CLAIM_FLAG_INFO = "message.cointcore.claim.flag.info";
    public static final String CLAIM_FLAG_MOB_SPAWN = "message.cointcore.claim.flag.mob_spawn";
    public static final String CLAIM_FLAG_MOB_SPAWN_MOB = "message.cointcore.claim.flag.mob_spawn.mob";
    public static final String CLAIM_FLAG_MOB_SPAWN_CLEARED = "message.cointcore.claim.flag.mob_spawn.cleared";
    public static final String CLAIM_FLAG_MOB_DAMAGE = "message.cointcore.claim.flag.mob_damage";
    public static final String CLAIM_FLAG_FIRE_SPREAD = "message.cointcore.claim.flag.fire_spread";
    public static final String CLAIM_FLAG_PVP = "message.cointcore.claim.flag.pvp";
    public static final String CLAIM_FLAG_ENTRY = "message.cointcore.claim.flag.entry";
    public static final String CLAIM_MOB_DAMAGE_BLOCKED = "message.cointcore.claim.mob_damage.blocked";
    public static final String CLAIM_FIRE_BLOCKED = "message.cointcore.claim.fire.blocked";
    public static final String CLAIM_ENTRY_DENIED = "message.cointcore.claim.entry.denied";
    public static final String CLAIM_BOSS_ARENA = "message.cointcore.claim.boss_arena";
    public static final String KIT_CREDIT_CLAIMED = "message.cointcore.kit.credit.claimed";
    public static final String KIT_CREDIT_ADDED = "message.cointcore.kit.credit.added";
    public static final String KIT_CREDIT_SET = "message.cointcore.kit.credit.set";
    public static final String KIT_CREDIT_TAKEN = "message.cointcore.kit.credit.taken";
    public static final String KIT_CREDIT_BALANCE_EMPTY = "message.cointcore.kit.credit.balance.empty";
    public static final String KIT_CREDIT_BALANCE_HEADER = "message.cointcore.kit.credit.balance.header";
    public static final String KIT_CREDIT_BALANCE_ENTRY = "message.cointcore.kit.credit.balance.entry";
    public static final String KIT_CREDIT_PLAYER_NOT_FOUND = "message.cointcore.kit.credit.player_not_found";
    public static final String KIT_UPDATED = "message.cointcore.kit.updated";
    public static final String VOTE_STARTED = "message.cointcore.vote.started";
    public static final String VOTE_PROGRESS = "message.cointcore.vote.progress";
    public static final String VOTE_PASSED = "message.cointcore.vote.passed";
    public static final String VOTE_ALREADY_VOTED = "message.cointcore.vote.already_voted";
    public static final String VOTE_COOLDOWN = "message.cointcore.vote.cooldown";
    public static final String ENVIRONMENT_COOLDOWN = "message.cointcore.environment.cooldown";
    public static final String ENVIRONMENT_COOLDOWN_BLOCKED = "message.cointcore.environment.cooldown_blocked";
    public static final String VOTE_TYPE_DAY = "message.cointcore.vote.type.day";
    public static final String VOTE_TYPE_CLEAR_WEATHER = "message.cointcore.vote.type.clear_weather";
    public static final String SERVER_RESTART_WARNING = "message.cointcore.server.restart.warning";
    public static final String SERVER_RESTART_SCHEDULED = "message.cointcore.server.restart.scheduled";
    public static final String SERVER_RESTART_MANUAL_SCHEDULED = "message.cointcore.server.restart.scheduled_manual";
    public static final String SERVER_RESTART_CANCELLED = "message.cointcore.server.restart.cancelled";
    public static final String SERVER_RESTART_NO_PENDING = "message.cointcore.server.restart.no_pending";
    public static final String SERVER_RESTART_INVALID_DURATION = "message.cointcore.server.restart.invalid_duration";
    public static final String SERVER_RESTART_NOW = "message.cointcore.server.restart.now";
    public static final String SERVER_RESTART_FAILED = "message.cointcore.server.restart.failed";
    public static final String SERVER_RESTART_KICK = "message.cointcore.server.restart.kick";
    public static final String DIMWIPE_WARNING = "message.cointcore.dimwipe.warning";
    public static final String DIMWIPE_STARTING = "message.cointcore.dimwipe.starting";
    public static final String DIMWIPE_RESTART_SCHEDULED = "message.cointcore.dimwipe.restart_scheduled";
    public static final String DIMWIPE_STATUS = "message.cointcore.dimwipe.status";
    public static final String DIMWIPE_NOW_OK = "message.cointcore.dimwipe.now.ok";
    public static final String DIMWIPE_NOW_FAILED = "message.cointcore.dimwipe.now.failed";
    public static final String DIMWIPE_DISABLED = "message.cointcore.dimwipe.disabled";
    public static final String CHAT_SPY_ENABLED = "message.cointcore.spy.enabled";
    public static final String CHAT_SPY_DISABLED = "message.cointcore.spy.disabled";
    public static final String CHAT_SPY_PM = "message.cointcore.spy.pm";
    public static final String CHAT_SPY_LOCAL = "message.cointcore.spy.local";
    public static final String CHAT_SPY_GLOBAL = "message.cointcore.spy.global";
    public static final String CHAT_SPY_LOCAL_RAW = "message.cointcore.spy.local_raw";
    public static final String ADMIN_CHAT = "message.cointcore.adminchat.message";
    public static final String ADMIN_CHAT_DISABLED = "message.cointcore.adminchat.disabled";
    public static final String AFK_WARN = "message.cointcore.afk.warn";
    public static final String AFK_KICK = "message.cointcore.afk.kick";
    public static final String AFK_SUSPECT = "message.cointcore.afk.suspect";
    public static final String AFK_SUSPECT_TP = "message.cointcore.afk.suspect.tp";
    public static final String AFK_SUSPECT_TP_HOVER = "message.cointcore.afk.suspect.tp.hover";
    public static final String INVSEE_PLAYER_ONLY = "message.cointcore.invsee.player_only";
    public static final String INVSEE_PLAYER_NOT_FOUND = "message.cointcore.invsee.player_not_found";
    public static final String INVSEE_CANNOT_SELF = "message.cointcore.invsee.cannot_self";
    public static final String INVSEE_OPENED_ONLINE = "message.cointcore.invsee.opened.online";
    public static final String INVSEE_OPENED_OFFLINE = "message.cointcore.invsee.opened.offline";
    public static final String INVSEE_FAILED = "message.cointcore.invsee.failed";
    public static final String INVSEE_NO_OFFLINE = "message.cointcore.invsee.no_offline";
    public static final String INVSEE_NO_SECTION = "message.cointcore.invsee.no_section";
    public static final String INVSEE_EXEMPT = "message.cointcore.invsee.exempt";
    public static final String INVSEE_BUSY = "message.cointcore.invsee.busy";
    public static final String INVSEE_TARGET_ONLINE = "message.cointcore.invsee.target.online";
    public static final String INVSEE_TARGET_OFFLINE = "message.cointcore.invsee.target.offline";
    public static final String STARTER_KIT_FIRST_JOIN = "message.cointcore.starter.first_join";
    public static final String STARTER_KIT_CLAIMED = "message.cointcore.starter.claimed";
    public static final String STARTER_KIT_DISABLED = "message.cointcore.starter.disabled";
    public static final String STARTER_KIT_MISSING = "message.cointcore.starter.missing";
    public static final String STARTER_KIT_FTB_MISSING = "message.cointcore.starter.ftb_missing";
    public static final String STARTER_KIT_EMPTY_INV = "message.cointcore.starter.empty_inv";
    public static final String STARTER_KIT_SET_FROM_INV = "message.cointcore.starter.set_from_inv";
    public static final String STARTER_KIT_STATUS = "message.cointcore.starter.status";
    public static final String STARTER_KIT_COOLDOWN_SYNCED = "message.cointcore.starter.cooldown_synced";
    public static final String STARTER_KIT_FIRSTJOIN_RESET = "message.cointcore.starter.firstjoin_reset";
    public static final String ITEM_PILE_NAME = "entity.cointcore.item_pile.name";
    public static final String WATCHDOG_DISABLED = "message.cointcore.watchdog.disabled";
    public static final String WATCHDOG_NO_DATA = "message.cointcore.watchdog.no_data";
    public static final String WATCHDOG_PLAYER_ONLY = "message.cointcore.watchdog.player_only";
    public static final String WATCHDOG_NO_ENTRY = "message.cointcore.watchdog.no_entry";
    public static final String WATCHDOG_DIM_MISSING = "message.cointcore.watchdog.dim_missing";
    public static final String WATCHDOG_TP = "message.cointcore.watchdog.tp";
    public static final String WATCHDOG_STARTED = "message.cointcore.watchdog.started";
    public static final String WATCHDOG_STOPPED = "message.cointcore.watchdog.stopped";
    public static final String WATCHDOG_REPORT_TRUNCATED = "message.cointcore.watchdog.report_truncated";
    public static final String GLUONS_GET = "message.cointcore.gluons.get";
    public static final String GLUONS_SET = "message.cointcore.gluons.set";
    public static final String GLUONS_ADD = "message.cointcore.gluons.add";
    public static final String GLUONS_PLAYER_NOT_FOUND = "message.cointcore.gluons.player_not_found";
    public static final String GLUONS_BALANCE = "message.cointcore.gluons.balance";
    public static final String GLUONS_PAY_SENT = "message.cointcore.gluons.pay.sent";
    public static final String GLUONS_PAY_RECEIVED = "message.cointcore.gluons.pay.received";
    public static final String GLUONS_PAY_SELF = "message.cointcore.gluons.pay.self";
    public static final String GLUONS_PAY_NOT_ENOUGH = "message.cointcore.gluons.pay.not_enough";
    public static final String TRADER_OFFER_UNAVAILABLE = "message.cointcore.trader.offer_unavailable";
    public static final String TRADER_INVENTORY_FULL = "message.cointcore.trader.inventory_full";
    public static final String TRADER_NOT_ENOUGH = "message.cointcore.trader.not_enough";
    public static final String TRADER_NOT_ENOUGH_ITEMS = "message.cointcore.trader.not_enough_items";
    public static final String TRADER_BOUGHT = "message.cointcore.trader.bought";
    public static final String TRADER_SOLD = "message.cointcore.trader.sold";
    public static final String PLAYER_SHOP_NO_USE = "message.cointcore.player_trader.no_use";
    public static final String PLAYER_SHOP_OUT_OF_STOCK = "message.cointcore.player_trader.out_of_stock";
    public static final String PLAYER_SHOP_STOCK_FULL = "message.cointcore.player_trader.stock_full";
    public static final String PLAYER_SHOP_OWNER_BROKE = "message.cointcore.player_trader.owner_broke";
    public static final String PLAYER_SHOP_OWN = "message.cointcore.player_trader.own_shop";

    private CointCoreMessages() {
    }

    public static Component forPlayer(ServerPlayer player, String key) {
        return LegacyTextParser.parse(translate(player.getLanguage(), key));
    }

    public static Component forPlayer(ServerPlayer player, String key, Object... args) {
        return LegacyTextParser.parse(String.format(translate(player.getLanguage(), key), args));
    }

    public static Component forConsole(String key) {
        return Component.literal(translate(DEFAULT_LANG, key));
    }

    public static Component forConsole(String key, Object... args) {
        return LegacyTextParser.parse(String.format(translate(DEFAULT_LANG, key), args));
    }

    public static Component forSource(CommandSourceStack source, String key) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return forPlayer(player, key);
        }

        return forConsole(key);
    }

    public static Component forSource(CommandSourceStack source, String key, Object... args) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return forPlayer(player, key, args);
        }

        return forConsole(key, args);
    }

    public static String translateKey(String language, String key) {
        return translate(language, key);
    }

    private static String translate(String language, String key) {
        String text = lookup(language, key);
        if (text != null) {
            return text;
        }

        text = lookup(DEFAULT_LANG, key);
        return text != null ? text : key;
    }

    private static String lookup(String language, String key) {
        Map<String, String> translations = TRANSLATIONS.get(normalizeLanguage(language));
        if (translations == null) {
            return null;
        }
        return translations.get(key);
    }

    private static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return DEFAULT_LANG;
        }
        return language.toLowerCase().replace('-', '_');
    }

    private static Map<String, Map<String, String>> loadTranslations() {
        Map<String, Map<String, String>> loaded = new HashMap<>();
        loadLanguage(loaded, "en_us");
        loadLanguage(loaded, "ru_ru");
        return Map.copyOf(loaded);
    }

    private static void loadLanguage(Map<String, Map<String, String>> loaded, String language) {
        String resourcePath = "/assets/" + CointCore.MOD_ID + "/lang/" + language + ".json";
        try (var input = CointCoreMessages.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                Map<String, String> translations = GSON.fromJson(reader, LANG_MAP_TYPE);
                if (translations != null && !translations.isEmpty()) {
                    loaded.put(language, translations);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
