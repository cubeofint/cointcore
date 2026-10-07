package com.mawlee.cointcore.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One durable currency movement for the site «движение валют» outbox.
 * {@code amount} is always positive; {@link #type()} and from/to encode direction.
 * {@link #deltas()} lists each player wallet changed by this movement.
 */
public record CurrencyMovement(
        long id,
        long timestampMs,
        UUID fromId,
        String fromName,
        UUID toId,
        String toName,
        long amount,
        CurrencyMovementType type,
        String note,
        List<Delta> deltas,
        String siteOpId
) {
    public CurrencyMovement(
            long id,
            long timestampMs,
            UUID fromId,
            String fromName,
            UUID toId,
            String toName,
            long amount,
            CurrencyMovementType type,
            String note
    ) {
        this(id, timestampMs, fromId, fromName, toId, toName, amount, type, note, List.of(), null);
    }

    public CurrencyMovement {
        amount = Math.max(0L, amount);
        deltas = deltas == null || deltas.isEmpty() ? List.of() : List.copyOf(deltas);
        siteOpId = siteOpId == null || siteOpId.isBlank() ? null : siteOpId;
    }

    public record Delta(UUID uuid, long delta, long balanceAfter) {
    }

    public CompoundTag save() {
        return writeMap(saveMap()).tag();
    }

    /**
     * Map-shaped NBT (no Minecraft types) for round-trip tests. Keys match the SavedData compound.
     */
    public Map<String, Object> saveMap() {
        Map<String, Object> tag = new LinkedHashMap<>();
        tag.put("id", id);
        tag.put("timestamp", timestampMs);
        if (fromId != null) {
            tag.put("from_id", fromId.toString());
        }
        if (fromName != null && !fromName.isBlank()) {
            tag.put("from_name", fromName);
        }
        if (toId != null) {
            tag.put("to_id", toId.toString());
        }
        if (toName != null && !toName.isBlank()) {
            tag.put("to_name", toName);
        }
        tag.put("amount", Math.max(0L, amount));
        tag.put("type", type.id());
        if (note != null && !note.isBlank()) {
            tag.put("note", note);
        }
        if (!deltas.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Delta delta : deltas) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("uuid", delta.uuid().toString());
                row.put("delta", delta.delta());
                row.put("balance_after", delta.balanceAfter());
                list.add(row);
            }
            tag.put("deltas", list);
        }
        if (siteOpId != null) {
            tag.put("site_op_id", siteOpId);
        }
        return tag;
    }

    public static CurrencyMovement load(CompoundTag tag) {
        return loadMap(readMap(tag));
    }

    public static CurrencyMovement loadMap(Map<String, Object> tag) {
        UUID fromId = uuid(tag.get("from_id"));
        UUID toId = uuid(tag.get("to_id"));
        List<Delta> deltas = List.of();
        Object rawDeltas = tag.get("deltas");
        if (rawDeltas instanceof List<?> list) {
            List<Delta> parsed = new ArrayList<>();
            for (Object row : list) {
                if (row instanceof Map<?, ?> map) {
                    UUID id = uuid(map.get("uuid"));
                    if (id != null) {
                        parsed.add(new Delta(id, asLong(map.get("delta")), asLong(map.get("balance_after"))));
                    }
                }
            }
            deltas = parsed;
        }
        return new CurrencyMovement(
                asLong(tag.get("id")),
                asLong(tag.get("timestamp")),
                fromId,
                string(tag.get("from_name")),
                toId,
                string(tag.get("to_name")),
                asLong(tag.get("amount")),
                CurrencyMovementType.fromId(string(tag.get("type"))),
                string(tag.get("note")),
                deltas,
                string(tag.get("site_op_id"))
        );
    }

    private static UUID uuid(Object raw) {
        if (raw instanceof UUID value) {
            return value;
        }
        if (raw instanceof String value && !value.isBlank()) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private static String string(Object raw) {
        return raw instanceof String value ? value : null;
    }

    private static long asLong(Object raw) {
        return raw instanceof Number number ? number.longValue() : 0L;
    }

    private static Map<String, Object> readMap(CompoundTag tag) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tag.getLong("id"));
        map.put("timestamp", tag.getLong("timestamp"));
        if (tag.hasUUID("from_id")) {
            map.put("from_id", tag.getUUID("from_id").toString());
        }
        if (tag.contains("from_name")) {
            map.put("from_name", tag.getString("from_name"));
        }
        if (tag.hasUUID("to_id")) {
            map.put("to_id", tag.getUUID("to_id").toString());
        }
        if (tag.contains("to_name")) {
            map.put("to_name", tag.getString("to_name"));
        }
        map.put("amount", tag.getLong("amount"));
        map.put("type", tag.getString("type"));
        if (tag.contains("note")) {
            map.put("note", tag.getString("note"));
        }
        if (tag.contains("deltas", Tag.TAG_LIST)) {
            List<Map<String, Object>> deltas = new ArrayList<>();
            ListTag list = tag.getList("deltas", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag row = list.getCompound(i);
                Map<String, Object> entry = new LinkedHashMap<>();
                if (row.hasUUID("uuid")) {
                    entry.put("uuid", row.getUUID("uuid").toString());
                } else if (row.contains("uuid")) {
                    entry.put("uuid", row.getString("uuid"));
                }
                entry.put("delta", row.getLong("delta"));
                entry.put("balance_after", row.getLong("balance_after"));
                deltas.add(entry);
            }
            map.put("deltas", deltas);
        }
        if (tag.contains("site_op_id")) {
            map.put("site_op_id", tag.getString("site_op_id"));
        }
        return map;
    }

    private static CompoundTagWriter writeMap(Map<String, Object> map) {
        return CompoundTagWriter.from(map);
    }

    /** Tiny adapter so production NBT and test maps share the same field names. */
    private record CompoundTagWriter(CompoundTag tag) {
        static CompoundTagWriter from(Map<String, Object> map) {
            CompoundTag tag = new CompoundTag();
            putLong(tag, map, "id");
            putLong(tag, map, "timestamp");
            putUuid(tag, map, "from_id");
            putString(tag, map, "from_name");
            putUuid(tag, map, "to_id");
            putString(tag, map, "to_name");
            putLong(tag, map, "amount");
            putString(tag, map, "type");
            putString(tag, map, "note");
            Object deltas = map.get("deltas");
            if (deltas instanceof List<?> list && !list.isEmpty()) {
                ListTag rows = new ListTag();
                for (Object row : list) {
                    if (row instanceof Map<?, ?> entry) {
                        CompoundTag compound = new CompoundTag();
                        Object uuid = entry.get("uuid");
                        if (uuid instanceof String value) {
                            try {
                                compound.putUUID("uuid", UUID.fromString(value));
                            } catch (IllegalArgumentException ignored) {
                                compound.putString("uuid", value);
                            }
                        }
                        if (entry.get("delta") instanceof Number number) {
                            compound.putLong("delta", number.longValue());
                        }
                        if (entry.get("balance_after") instanceof Number number) {
                            compound.putLong("balance_after", number.longValue());
                        }
                        rows.add(compound);
                    }
                }
                tag.put("deltas", rows);
            }
            putString(tag, map, "site_op_id");
            return new CompoundTagWriter(tag);
        }

        private static void putLong(CompoundTag tag, Map<String, Object> map, String key) {
            if (map.get(key) instanceof Number number) {
                tag.putLong(key, number.longValue());
            }
        }

        private static void putString(CompoundTag tag, Map<String, Object> map, String key) {
            if (map.get(key) instanceof String value) {
                tag.putString(key, value);
            }
        }

        private static void putUuid(CompoundTag tag, Map<String, Object> map, String key) {
            if (map.get(key) instanceof String value) {
                try {
                    tag.putUUID(key, UUID.fromString(value));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}
