package com.gmail.uprial.takeaim;

import com.gmail.uprial.takeaim.common.CustomLogger;
import com.gmail.uprial.takeaim.config.ConfigReaderSimple;
import com.gmail.uprial.takeaim.config.InvalidConfigException;
import org.bukkit.configuration.file.FileConfiguration;

public final class TakeAimConfig {
    private final boolean enabled;

    private TakeAimConfig(boolean enabled) {
        this.enabled = enabled;
    }

    static boolean isDebugMode(FileConfiguration config, CustomLogger customLogger) throws InvalidConfigException {
        return ConfigReaderSimple.getBoolean(config, customLogger, "debug", "'debug' flag", false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    static TakeAimConfig getFromConfig(FileConfiguration config, CustomLogger customLogger) throws InvalidConfigException {
        boolean enabled = ConfigReaderSimple.getBoolean(config, customLogger, "enabled", "'enabled' flag", true);

        return new TakeAimConfig(enabled);
    }

    public String toString() {
        return String.format("enabled: %b", enabled);
    }
}
