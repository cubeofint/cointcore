package com.mawlee.cointcore.watchdog;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

/**
 * Best-effort count of {@code MinecraftServer.execute} / {@code BlockableEventLoop} pending runnables.
 */
public final class ServerTaskQueueProbe {
    private ServerTaskQueueProbe() {
    }

    /**
     * @return pending task count, or {@code -1} if the queue could not be found
     */
    public static int pendingCount(Object server) {
        if (server == null) {
            return -1;
        }
        for (Class<?> type = server.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            Field[] fields;
            try {
                fields = type.getDeclaredFields();
            } catch (SecurityException ignored) {
                continue;
            }
            for (Field field : fields) {
                if (!isTaskCollection(field.getType()) || !looksLikeTaskQueue(field.getName())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(server);
                    if (value instanceof Collection<?> collection) {
                        return collection.size();
                    }
                    if (value instanceof Map<?, ?> map) {
                        return map.size();
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        return -1;
    }

    static boolean looksLikeTaskQueue(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }
        String name = fieldName.toLowerCase(Locale.ROOT);
        return name.contains("pending")
                || name.contains("runnable")
                || name.contains("task")
                || name.contains("queue");
    }

    private static boolean isTaskCollection(Class<?> type) {
        return type != null && (Queue.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type)
                || Map.class.isAssignableFrom(type));
    }
}
