package com.mawlee.cointcore.refinedstorage.wireless;

import com.google.gson.*;
import java.util.Optional;
import java.util.function.Function;

public final class PinBoardJson {
    private PinBoardJson() { }
    public static <T> JsonObject write(PinBoard<T> board, Function<T, JsonElement> encode) {
        var root = new JsonObject();
        root.addProperty("version", 1);
        for (var section : PinBoard.Section.values()) {
            var rows = new JsonArray();
            var cells = board.cells(section);
            for (int start = 0; start < cells.size(); start += PinBoard.COLUMNS) {
                var row = new JsonArray();
                for (int column = 0; column < PinBoard.COLUMNS; column++) {
                    T key = cells.get(start + column);
                    row.add(key == null ? JsonNull.INSTANCE : encode.apply(key));
                }
                rows.add(row);
            }
            root.add(section.name().toLowerCase(java.util.Locale.ROOT), rows);
        }
        return root;
    }
    public static <T> PinBoard<T> read(JsonObject root, Function<JsonElement, Optional<T>> decode) {
        var board = new PinBoard<T>();
        if (!root.has("version") || root.get("version").getAsInt() != 1) throw new IllegalArgumentException("Unsupported pin profile version");
        for (var section : PinBoard.Section.values()) {
            var rows = root.getAsJsonArray(section.name().toLowerCase(java.util.Locale.ROOT));
            if (rows == null) continue;
            for (var element : rows) {
                var row = element.getAsJsonArray();
                if (row.size() != PinBoard.COLUMNS) throw new IllegalArgumentException("A pin row must contain nine cells");
                int start = board.rows(section) * PinBoard.COLUMNS;
                board.addRow(section);
                for (int column = 0; column < row.size(); column++) {
                    var value = row.get(column);
                    if (!value.isJsonNull()) {
                        var key = decode.apply(value);
                        if (key.isPresent()) board.set(section, start + column, key.get());
                    }
                }
            }
        }
        return board;
    }
}
