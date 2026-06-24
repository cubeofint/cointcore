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
    private static final Type LANG_MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
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
    public static final String CHUNK_LIMIT_NOT_FOUND = "message.cointcore.chunklimit.not_found";
    public static final String CHUNK_LIMIT_CHECK_HEADER = "message.cointcore.chunklimit.check_header";
    public static final String CHUNK_LIMIT_CHECK_BLOCK_ENTRY = "message.cointcore.chunklimit.check_block_entry";
    public static final String CHUNK_LIMIT_CHECK_ENTITY_ENTRY = "message.cointcore.chunklimit.check_entity_entry";
    public static final String CHUNK_LIMIT_DUMP_SUCCESS = "message.cointcore.chunklimit.dump_success";
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
    public static final String CLAIM_FLAG_NO_PLAYER_DAMAGE_ENABLED = "message.cointcore.claim.flag.no_player_damage.enabled";
    public static final String CLAIM_FLAG_NO_PLAYER_DAMAGE_DISABLED = "message.cointcore.claim.flag.no_player_damage.disabled";
    public static final String CLAIM_FLAG_NO_HOSTILE_MOB_SPAWN_ENABLED = "message.cointcore.claim.flag.no_hostile_mob_spawn.enabled";
    public static final String CLAIM_FLAG_NO_HOSTILE_MOB_SPAWN_DISABLED = "message.cointcore.claim.flag.no_hostile_mob_spawn.disabled";
    public static final String CLAIM_FLAG_PROTECT_MOBS_ENABLED = "message.cointcore.claim.flag.protect_mobs.enabled";
    public static final String CLAIM_FLAG_PROTECT_MOBS_DISABLED = "message.cointcore.claim.flag.protect_mobs.disabled";
    public static final String CLAIM_MOB_DAMAGE_BLOCKED = "message.cointcore.claim.mob_damage.blocked";
    public static final String KIT_CREDIT_CLAIMED = "message.cointcore.kit.credit.claimed";
    public static final String KIT_CREDIT_ADDED = "message.cointcore.kit.credit.added";
    public static final String KIT_CREDIT_SET = "message.cointcore.kit.credit.set";
    public static final String KIT_CREDIT_TAKEN = "message.cointcore.kit.credit.taken";
    public static final String KIT_CREDIT_BALANCE_EMPTY = "message.cointcore.kit.credit.balance.empty";
    public static final String KIT_CREDIT_BALANCE_HEADER = "message.cointcore.kit.credit.balance.header";
    public static final String KIT_CREDIT_BALANCE_ENTRY = "message.cointcore.kit.credit.balance.entry";
    public static final String KIT_CREDIT_PLAYER_NOT_FOUND = "message.cointcore.kit.credit.player_not_found";
    public static final String VOTE_STARTED = "message.cointcore.vote.started";
    public static final String VOTE_PROGRESS = "message.cointcore.vote.progress";
    public static final String VOTE_PASSED = "message.cointcore.vote.passed";
    public static final String VOTE_ALREADY_VOTED = "message.cointcore.vote.already_voted";
    public static final String VOTE_COOLDOWN = "message.cointcore.vote.cooldown";
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
    public static final String CHAT_SPY_ENABLED = "message.cointcore.spy.enabled";
    public static final String CHAT_SPY_DISABLED = "message.cointcore.spy.disabled";
    public static final String CHAT_SPY_PM = "message.cointcore.spy.pm";
    public static final String CHAT_SPY_LOCAL = "message.cointcore.spy.local";
    public static final String CHAT_SPY_GLOBAL = "message.cointcore.spy.global";
    public static final String CHAT_SPY_LOCAL_RAW = "message.cointcore.spy.local_raw";
    public static final String ADMIN_CHAT = "message.cointcore.adminchat.message";
    public static final String ADMIN_CHAT_DISABLED = "message.cointcore.adminchat.disabled";

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
