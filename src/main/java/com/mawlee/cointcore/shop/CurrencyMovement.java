package com.mawlee.cointcore.shop;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * One durable currency movement for the site «движение валют» outbox.
 * {@code amount} is always positive; {@link #type()} and from/to encode direction.
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
        String note
) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("id", id);
        tag.putLong("timestamp", timestampMs);
        if (fromId != null) {
            tag.putUUID("from_id", fromId);
        }
        if (fromName != null && !fromName.isBlank()) {
            tag.putString("from_name", fromName);
        }
        if (toId != null) {
            tag.putUUID("to_id", toId);
        }
        if (toName != null && !toName.isBlank()) {
            tag.putString("to_name", toName);
        }
        tag.putLong("amount", Math.max(0L, amount));
        tag.putString("type", type.id());
        if (note != null && !note.isBlank()) {
            tag.putString("note", note);
        }
        return tag;
    }

    public static CurrencyMovement load(CompoundTag tag) {
        UUID fromId = tag.hasUUID("from_id") ? tag.getUUID("from_id") : null;
        UUID toId = tag.hasUUID("to_id") ? tag.getUUID("to_id") : null;
        return new CurrencyMovement(
                tag.getLong("id"),
                tag.getLong("timestamp"),
                fromId,
                tag.contains("from_name") ? tag.getString("from_name") : null,
                toId,
                tag.contains("to_name") ? tag.getString("to_name") : null,
                Math.max(0L, tag.getLong("amount")),
                CurrencyMovementType.fromId(tag.getString("type")),
                tag.contains("note") ? tag.getString("note") : null
        );
    }
}
