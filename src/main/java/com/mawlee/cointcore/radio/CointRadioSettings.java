package com.mawlee.cointcore.radio;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class CointRadioSettings {
    public static ModConfigSpec.BooleanValue RADIO_ENABLED;
    public static ModConfigSpec.ConfigValue<String> RADIO_DEFAULT_STATION;
    public static ModConfigSpec.ConfigValue<List<? extends String>> RADIO_STATIONS;
    public static ModConfigSpec.ConfigValue<String> RADIO_ON_MESSAGE;
    public static ModConfigSpec.ConfigValue<String> RADIO_OFF_MESSAGE;
    public static ModConfigSpec.IntValue RADIO_RADIUS;
    public static final ModConfigSpec SPEC;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("radio");

        RADIO_ENABLED = builder
                .comment("Enable CointCore radio block.")
                .define("enabled", true);

        RADIO_RADIUS = builder
                .comment("Radio block radius in blocks.")
                .defineInRange("radius", 24, 1, 128);

        RADIO_DEFAULT_STATION = builder.comment("Default station id from stations list.")
                .define("default_station", "dorognoe");

        RADIO_STATIONS = builder.defineList(
                "stations",
                List.of(
                        "dorognoe|Дорожное радио|http://dorognoe.hostingradio.ru:8000/dorognoe",
                        "europa_plus|Европа Плюс|http://ep128.hostingradio.ru:8030/ep128",
                        "shanson|Радио Шансон|http://chanson.hostingradio.ru:8041/chanson128.mp3",

                        "soma_groove|SomaFM Groove Salad|https://somafm.com/groovesalad.pls",
                        "soma_drone|SomaFM Drone Zone|https://somafm.com/dronezone.pls",
                        "soma_defcon|SomaFM DEF CON Radio|https://somafm.com/defcon.pls",
                        "soma_lush|SomaFM Lush|https://somafm.com/lush.pls",
                        "soma_secret|SomaFM Secret Agent|https://somafm.com/secretagent.pls",
                        "soma_indiepop|SomaFM Indie Pop Rocks|https://somafm.com/indiepop.pls"
                ),
                value -> {
                    if (!(value instanceof String string)) {
                        return false;
                    }

                    String trimmed = string.trim();

                    if (trimmed.isBlank()) {
                        return false;
                    }

                    if (trimmed.contains("=")) {
                        String[] parts = trimmed.split("=", 2);

                        return parts.length == 2
                                && !parts[0].trim().isBlank()
                                && isValidRadioUrl(parts[1].trim());
                    }

                    if (trimmed.contains("|")) {
                        String[] parts = trimmed.split("\\|", 3);

                        return parts.length == 3
                                && !parts[0].trim().isBlank()
                                && !parts[1].trim().isBlank()
                                && isValidRadioUrl(parts[2].trim());
                    }

                    return false;
                }
        );

        RADIO_ON_MESSAGE = builder
                .comment("Actionbar message when radio starts. Use %station% for station id.")
                .define("on_message", "§a[CointMusic] Радио включено: §f%station%");

        RADIO_OFF_MESSAGE = builder
                .comment("Actionbar message when radio stops.")
                .define("off_message", "§c[CointMusic] Радио выключено.");

        builder.pop();

        SPEC=builder.build();
    }
    private static boolean isValidRadioUrl(String url) {
        try { var uri=java.net.URI.create(url); return uri.getHost()!=null && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())); } catch (IllegalArgumentException error) { return false; }
    }
}
