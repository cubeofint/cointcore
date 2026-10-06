package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClaimTeamFlagsSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_claim_flags";
    private static final String TEAMS_KEY = "teams";

    private final Map<UUID, LegacyFlags> flagsByTeam = new HashMap<>();

    private ClaimTeamFlagsSavedData() {
    }

    public static ClaimTeamFlagsSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new Factory<>(ClaimTeamFlagsSavedData::new, ClaimTeamFlagsSavedData::load), DATA_ID);
    }

    public boolean hadNoPlayerDamage(UUID teamId) {
        LegacyFlags legacy = flagsByTeam.get(teamId);
        return legacy != null && legacy.noPlayerDamage();
    }

    public boolean isEmpty() {
        return flagsByTeam.isEmpty();
    }

    public Iterable<UUID> getTeamIds() {
        return flagsByTeam.keySet();
    }

    public void clear() {
        if (!flagsByTeam.isEmpty()) {
            flagsByTeam.clear();
            setDirty();
        }
    }

    private static ClaimTeamFlagsSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ClaimTeamFlagsSavedData data = new ClaimTeamFlagsSavedData();
        ListTag teams = tag.getList(TEAMS_KEY, Tag.TAG_COMPOUND);

        for (Tag entryTag : teams) {
            CompoundTag entry = (CompoundTag) entryTag;
            UUID teamId = entry.getUUID("team_id");
            data.flagsByTeam.put(
                    teamId,
                    new LegacyFlags(
                            entry.getBoolean("no_player_damage"),
                            entry.getBoolean("protect_mobs")
                    )
            );
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag teams = new ListTag();
        for (Map.Entry<UUID, LegacyFlags> entry : flagsByTeam.entrySet()) {
            CompoundTag teamTag = new CompoundTag();
            teamTag.putUUID("team_id", entry.getKey());
            teamTag.putBoolean("no_player_damage", entry.getValue().noPlayerDamage());
            teamTag.putBoolean("protect_mobs", entry.getValue().protectMobsFromOutsiders());
            teams.add(teamTag);
        }

        tag.put(TEAMS_KEY, teams);
        return tag;
    }

    private record LegacyFlags(boolean noPlayerDamage, boolean protectMobsFromOutsiders) {
    }
}
