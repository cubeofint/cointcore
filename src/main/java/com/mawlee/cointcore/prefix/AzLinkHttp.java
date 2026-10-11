package com.mawlee.cointcore.prefix;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * Reflection-safe AzLink HTTP used by prefix sync. There is no compile dependency on AzLink.
 *
 * <p>Preferred public API on {@code com.azuriom.azlink.common.coins.CoinOperationsBridge}
 * (add in AzLink-mods if missing):
 * <ul>
 *   <li>{@code static boolean isAvailable()}</li>
 *   <li>{@code static CompletableFuture<String> postJson(String path, String json)}</li>
 *   <li>{@code static CompletableFuture<String> getJson(String path)}</li>
 * </ul>
 * Optional dedicated methods: {@code reportPrefixes}, {@code fetchPrefixOperations}, {@code ackPrefix}.
 *
 * <p>AzLink {@code HttpClient.request} builds {@code siteUrl + "/api" + path} and authenticates with
 * the AzLink token. Call paths as {@code /azlink/prefixes/...} so the site sees
 * {@code /api/azlink/prefixes/...}.
 *
 * <p>If those methods are absent, this class uses {@code CoinOperationsBridge.plugin}
 * → {@code getHttpClient().request(...)} — the same client {@code postMovements} already uses.
 */
public final class AzLinkHttp {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final String BRIDGE = "com.azuriom.azlink.common.coins.CoinOperationsBridge";
    static final String HTTP_CLIENT = "com.azuriom.azlink.common.http.client.HttpClient";
    static final String REPORT_PATH = "/azlink/prefixes/report";
    static final String OPERATIONS_PATH = "/azlink/prefixes/operations";
    static final String ACK_PATH = "/azlink/prefixes/ack";
    static final String CATALOG_PATH = "/azlink/prefixes/catalog";

    private static boolean resolved;
    private static Method isAvailable;
    private static Method postJson;
    private static Method getJson;
    private static Method reportPrefixes;
    private static Method fetchPrefixOperations;
    private static Method ackPrefix;
    private static Method httpRequest;
    private static Field pluginField;
    private static Object postMethod;
    private static Object getMethod;
    private static boolean loggedMissing;

    private AzLinkHttp() {
    }

    public static boolean isAvailable() {
        if (!resolve() || isAvailable == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(isAvailable.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    public static CompletableFuture<String> postJson(String path, String json) {
        if (!resolve()) {
            return CompletableFuture.failedFuture(new IllegalStateException("AzLink bridge not found"));
        }
        try {
            if (postJson != null) {
                @SuppressWarnings("unchecked")
                CompletableFuture<String> future = (CompletableFuture<String>) postJson.invoke(null, path, json);
                return future;
            }
            return request("POST", path, json);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    public static CompletableFuture<String> getJson(String path) {
        if (!resolve()) {
            return CompletableFuture.failedFuture(new IllegalStateException("AzLink bridge not found"));
        }
        try {
            if (getJson != null) {
                @SuppressWarnings("unchecked")
                CompletableFuture<String> future = (CompletableFuture<String>) getJson.invoke(null, path);
                return future;
            }
            return request("GET", path, null);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    public static CompletableFuture<String> reportPrefixes(String json) {
        if (resolve() && reportPrefixes != null) {
            try {
                @SuppressWarnings("unchecked")
                CompletableFuture<String> future = (CompletableFuture<String>) reportPrefixes.invoke(null, json);
                return future;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }
        return postJson(REPORT_PATH, json);
    }

    public static CompletableFuture<String> fetchPrefixOperations(int limit) {
        if (resolve() && fetchPrefixOperations != null) {
            try {
                @SuppressWarnings("unchecked")
                CompletableFuture<String> future =
                        (CompletableFuture<String>) fetchPrefixOperations.invoke(null, limit);
                return future;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }
        String path = OPERATIONS_PATH + "?limit=" + Math.max(1, limit);
        return getJson(path);
    }

    public static CompletableFuture<String> exportCatalog(String json) {
        return postJson(CATALOG_PATH, json);
    }

    public static CompletableFuture<String> ackPrefix(String opId, String status, String reason, String selected) {
        if (resolve() && ackPrefix != null) {
            try {
                @SuppressWarnings("unchecked")
                CompletableFuture<String> future =
                        (CompletableFuture<String>) ackPrefix.invoke(null, opId, status, reason, selected);
                return future;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }
        JsonObject body = new JsonObject();
        body.addProperty("op_id", opId);
        body.addProperty("status", status);
        if (reason != null) {
            body.addProperty("reason", reason);
        }
        if (selected == null) {
            body.add("selected", null);
        } else {
            body.addProperty("selected", selected);
        }
        return postJson(ACK_PATH, body.toString());
    }

    private static CompletableFuture<String> request(String methodName, String path, String json)
            throws ReflectiveOperationException {
        if (httpRequest == null || pluginField == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("AzLink HttpClient.request missing"));
        }
        Object plugin = pluginField.get(null);
        if (plugin == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("AzLink plugin not installed"));
        }
        Object http = plugin.getClass().getMethod("getHttpClient").invoke(plugin);
        Object method = "GET".equals(methodName) ? getMethod : postMethod;
        Object body = json == null || json.isBlank() ? null : JsonParser.parseString(json);
        @SuppressWarnings("unchecked")
        CompletableFuture<Object> future = (CompletableFuture<Object>) httpRequest.invoke(
                http,
                method,
                path,
                body,
                JsonObject.class
        );
        return future.thenApply(value -> value == null ? "{}" : value.toString());
    }

    private static synchronized boolean resolve() {
        if (resolved) {
            return isAvailable != null;
        }
        resolved = true;
        try {
            Class<?> bridge = Class.forName(BRIDGE, true, AzLinkHttp.class.getClassLoader());
            isAvailable = bridge.getMethod("isAvailable");
            postJson = optionalMethod(bridge, "postJson", String.class, String.class);
            getJson = optionalMethod(bridge, "getJson", String.class);
            reportPrefixes = optionalMethod(bridge, "reportPrefixes", String.class);
            fetchPrefixOperations = optionalMethod(bridge, "fetchPrefixOperations", int.class);
            ackPrefix = optionalMethod(bridge, "ackPrefix", String.class, String.class, String.class, String.class);
            resolveHttpClient(bridge);
            LOGGER.info("AzLink prefix HTTP ready (postJson={}, getJson={}, HttpClient.request={})",
                    postJson != null, getJson != null, httpRequest != null);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!loggedMissing) {
                loggedMissing = true;
                LOGGER.warn(
                        "AzLink {} not found; prefix site sync disabled. AzLink-mods must expose "
                                + "CoinOperationsBridge.postJson/getJson or keep CoinOperationsBridge.plugin + "
                                + "HttpClient.request. Paths: {} {} {} {}",
                        BRIDGE,
                        REPORT_PATH,
                        OPERATIONS_PATH,
                        ACK_PATH,
                        CATALOG_PATH
                );
            }
            return false;
        }
    }

    private static void resolveHttpClient(Class<?> bridge) {
        try {
            Field field = bridge.getDeclaredField("plugin");
            field.setAccessible(true);
            pluginField = field;
            Class<?> http = Class.forName(HTTP_CLIENT, true, AzLinkHttp.class.getClassLoader());
            Class<?> methodEnum = Class.forName(HTTP_CLIENT + "$RequestMethod", true, AzLinkHttp.class.getClassLoader());
            postMethod = Enum.valueOf(methodEnum.asSubclass(Enum.class), "POST");
            getMethod = Enum.valueOf(methodEnum.asSubclass(Enum.class), "GET");
            httpRequest = http.getMethod("request", methodEnum, String.class, Object.class, Class.class);
        } catch (ReflectiveOperationException ignored) {
            httpRequest = null;
        }
    }

    private static Method optionalMethod(Class<?> owner, String name, Class<?>... parameters) {
        try {
            return owner.getMethod(name, parameters);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
