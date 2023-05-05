package com.gmail.uprial.takeaim;

import com.gmail.uprial.takeaim.common.CustomLogger;
import com.gmail.uprial.takeaim.config.ConfigReaderSimple;
import com.gmail.uprial.takeaim.config.InvalidConfigException;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;

import static com.gmail.uprial.takeaim.config.ConfigReaderEnums.getStringSet;

public final class TakeAimConfig {
    private final boolean enabled;
    private final Set<String> worlds;

    private TakeAimConfig(boolean enabled, Set<String> worlds) {
        this.enabled = enabled;
        this.worlds = worlds;
    }

    static boolean isDebugMode(FileConfiguration config, CustomLogger customLogger) throws InvalidConfigException {
        return ConfigReaderSimple.getBoolean(config, customLogger, "debug", "'debug' flag", false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isWorldEnabled(String world) {
        if (worlds == null) {
            return true;
        } else {
            return worlds.contains(world);
        }
    }

    public static TakeAimConfig getFromConfig(FileConfiguration config, CustomLogger customLogger) throws InvalidConfigException {
        final boolean enabled = ConfigReaderSimple.getBoolean(config, customLogger, "enabled", "'enabled' flag", true);
        final Set<String> worlds = getStringSet(config, customLogger, "worlds", "worlds");

        return new TakeAimConfig(enabled, worlds);
    }

    public String toString() {
        return String.format("enabled: %b, worlds: %s",
                enabled, worlds);
    }
}
