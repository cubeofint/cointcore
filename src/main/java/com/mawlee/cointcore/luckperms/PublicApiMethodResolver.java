package com.mawlee.cointcore.luckperms;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Looks up reflective methods on a public API type (interface or superclass)
 * so JPMS can invoke them on non-public LuckPerms implementation classes.
 */
public final class PublicApiMethodResolver {
    private PublicApiMethodResolver() {
    }

    public static Method resolve(Class<?> runtimeType, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        if (runtimeType == null) {
            throw new NoSuchMethodException(methodName);
        }
        for (Class<?> type : publicTypes(runtimeType)) {
            try {
                return type.getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException ignored) {
            }
        }
        Method fallback = runtimeType.getMethod(methodName, parameterTypes);
        fallback.setAccessible(true);
        return fallback;
    }

    public static Method resolveByCount(Class<?> runtimeType, String methodName, int parameterCount)
            throws NoSuchMethodException {
        if (runtimeType == null) {
            throw new NoSuchMethodException(methodName);
        }
        for (Class<?> type : publicTypes(runtimeType)) {
            Method match = firstMatching(type, methodName, parameterCount);
            if (match != null) {
                return match;
            }
        }
        Method fallback = firstMatching(runtimeType, methodName, parameterCount);
        if (fallback != null) {
            fallback.setAccessible(true);
            return fallback;
        }
        throw new NoSuchMethodException(runtimeType.getName() + "#" + methodName);
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
     * Public interfaces first (BFS), then public superclasses, then the runtime type if it is public.
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
        result.addAll(interfaces);
        result.addAll(classes);
        if (isPublicType(runtimeType)) {
            result.add(runtimeType);
        }
        return result;
    }

    static boolean isPublicType(Class<?> type) {
        return type != null && Modifier.isPublic(type.getModifiers());
    }
}
