package com.mawlee.cointcore.luckperms;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Looks up reflective methods on public API types (interfaces and superclasses)
 * so JPMS can invoke them on non-public LuckPerms implementation classes.
 *
 * <p>Never falls back to {@code getMethod} on the runtime impl class. Lookup walks
 * every public interface and superinterface (BFS), then public superclasses,
 * using exact {@link Class#getMethod} first and assignable-parameter overload
 * matching second (needed after generic erasure, e.g.
 * {@code NodeBuilder.withMetadata(NodeMetadataKey, Object)}).
 *
 * <p>Resolved {@link Method}s are cached.
 */
public final class PublicApiMethodResolver {
    private static final ConcurrentHashMap<CacheKey, Method> CACHE = new ConcurrentHashMap<>();

    private PublicApiMethodResolver() {
    }

    public static Method resolve(Class<?> runtimeType, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        if (runtimeType == null) {
            throw new NoSuchMethodException(methodName);
        }
        CacheKey key = new CacheKey(runtimeType, methodName, typesKey(parameterTypes));
        Method cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Method resolved = resolveUncached(runtimeType, methodName, parameterTypes);
        Method raced = CACHE.putIfAbsent(key, resolved);
        return raced != null ? raced : resolved;
    }

    public static Method resolveByCount(Class<?> runtimeType, String methodName, int parameterCount)
            throws NoSuchMethodException {
        if (runtimeType == null) {
            throw new NoSuchMethodException(methodName);
        }
        CacheKey key = new CacheKey(runtimeType, methodName, countKey(parameterCount));
        Method cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Method resolved = resolveByCountUncached(runtimeType, methodName, parameterCount);
        Method raced = CACHE.putIfAbsent(key, resolved);
        return raced != null ? raced : resolved;
    }

    public static Object invoke(Object target, String methodName, Object... args)
            throws ReflectiveOperationException {
        return resolveByCount(target.getClass(), methodName, args.length).invoke(target, args);
    }

    public static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws ReflectiveOperationException {
        return resolve(target.getClass(), methodName, parameterTypes).invoke(target, args);
    }

    public static Object invoke(Class<?> owner, Object target, String methodName, Object... args)
            throws ReflectiveOperationException {
        Class<?> type = owner != null ? owner : target.getClass();
        Method method = args.length == 0
                ? resolve(type, methodName)
                : resolveByCount(type, methodName, args.length);
        return method.invoke(target, args);
    }

    static int cacheSize() {
        return CACHE.size();
    }

    static void clearCache() {
        CACHE.clear();
    }

    private static Method resolveUncached(Class<?> runtimeType, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        List<Class<?>> types = publicTypes(runtimeType);
        for (Class<?> type : types) {
            try {
                return type.getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException ignored) {
            }
        }
        Method assignable = resolveAssignable(types, methodName, parameterTypes);
        if (assignable != null) {
            return assignable;
        }
        throw new NoSuchMethodException(describe(runtimeType, methodName, parameterTypes));
    }

    private static Method resolveByCountUncached(Class<?> runtimeType, String methodName, int parameterCount)
            throws NoSuchMethodException {
        for (Class<?> type : publicTypes(runtimeType)) {
            Method match = firstMatching(type, methodName, parameterCount);
            if (match != null) {
                return match;
            }
        }
        throw new NoSuchMethodException(runtimeType.getName() + "#" + methodName + "/" + parameterCount);
    }

    private static Method resolveAssignable(List<Class<?>> types, String methodName, Class<?>[] parameterTypes) {
        Method best = null;
        int bestScore = -1;
        for (Class<?> type : types) {
            for (Method method : type.getMethods()) {
                if (!method.getName().equals(methodName) || !isPublicType(method.getDeclaringClass())) {
                    continue;
                }
                Class<?>[] formals = method.getParameterTypes();
                if (formals.length != parameterTypes.length) {
                    continue;
                }
                int score = compatibilityScore(formals, parameterTypes);
                if (score > bestScore) {
                    best = method;
                    bestScore = score;
                }
            }
        }
        return best;
    }

    private static int compatibilityScore(Class<?>[] formals, Class<?>[] actuals) {
        int score = 0;
        for (int index = 0; index < formals.length; index++) {
            if (!isCompatible(formals[index], actuals[index])) {
                return -1;
            }
            if (box(formals[index]) == box(actuals[index])) {
                score += 2;
            } else {
                score += 1;
            }
        }
        return score;
    }

    static boolean isCompatible(Class<?> formal, Class<?> actual) {
        if (actual == null) {
            return !formal.isPrimitive();
        }
        if (formal.isAssignableFrom(actual)) {
            return true;
        }
        return box(formal).isAssignableFrom(box(actual));
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == void.class) {
            return Void.class;
        }
        return type;
    }

    private static Method firstMatching(Class<?> type, String methodName, int parameterCount) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != parameterCount) {
                continue;
            }
            if (isPublicType(method.getDeclaringClass())) {
                return method;
            }
        }
        return null;
    }

    /**
     * The runtime type first when it is public (so {@code InheritanceNode.builder(String)}
     * wins over generic {@code Node.builder(String)}), then public interfaces (BFS of every
     * superinterface), then public superclasses.
     */
    static List<Class<?>> publicTypes(Class<?> runtimeType) {
        List<Class<?>> interfaces = new ArrayList<>();
        List<Class<?>> classes = new ArrayList<>();
        Set<Class<?>> seen = new LinkedHashSet<>();
        Deque<Class<?>> queue = new ArrayDeque<>();
        queue.add(runtimeType);
        while (!queue.isEmpty()) {
            Class<?> current = queue.removeFirst();
            if (current == null || !seen.add(current)) {
                continue;
            }
            if (current != runtimeType && isPublicType(current)) {
                if (current.isInterface()) {
                    interfaces.add(current);
                } else {
                    classes.add(current);
                }
            }
            for (Class<?> iface : current.getInterfaces()) {
                queue.add(iface);
            }
            Class<?> superclass = current.getSuperclass();
            if (superclass != null) {
                queue.add(superclass);
            }
        }
        List<Class<?>> result = new ArrayList<>(interfaces.size() + classes.size() + 1);
        if (isPublicType(runtimeType)) {
            result.add(runtimeType);
        }
        result.addAll(interfaces);
        result.addAll(classes);
        return result;
    }

    static boolean isPublicType(Class<?> type) {
        return type != null && Modifier.isPublic(type.getModifiers());
    }

    private static String typesKey(Class<?>[] parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0) {
            return "()";
        }
        StringBuilder builder = new StringBuilder(parameterTypes.length * 24);
        builder.append('(');
        for (Class<?> type : parameterTypes) {
            builder.append(type == null ? "null" : type.getName()).append(',');
        }
        builder.append(')');
        return builder.toString();
    }

    private static String countKey(int parameterCount) {
        return "#" + parameterCount;
    }

    private static String describe(Class<?> runtimeType, String methodName, Class<?>[] parameterTypes) {
        StringBuilder builder = new StringBuilder(runtimeType.getName())
                .append('#')
                .append(methodName)
                .append('(');
        if (parameterTypes != null) {
            for (int index = 0; index < parameterTypes.length; index++) {
                if (index > 0) {
                    builder.append(',');
                }
                builder.append(parameterTypes[index] == null ? "null" : parameterTypes[index].getName());
            }
        }
        return builder.append(") not found on public API types").toString();
    }

    private record CacheKey(Class<?> type, String name, String signature) {
    }
}
