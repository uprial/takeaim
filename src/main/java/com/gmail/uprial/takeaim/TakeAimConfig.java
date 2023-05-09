package com.gmail.uprial.takeaim;

import com.gmail.uprial.takeaim.common.CustomLogger;
import com.gmail.uprial.takeaim.config.ConfigReaderSimple;
import com.gmail.uprial.takeaim.config.InvalidConfigException;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;

import static com.gmail.uprial.takeaim.config.ConfigReaderEnums.*;

public final class TakeAimConfig {
    private final boolean enabled;
    private final Set<String> worlds;
    private final Set<Biome> excludeBiomes;

    private TakeAimConfig(final boolean enabled, final Set<String> worlds, final Set<Biome> excludeBiomes) {
        this.enabled = enabled;
        this.worlds = worlds;
        this.excludeBiomes = excludeBiomes;
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

    public boolean isBiomeEnabled(Biome biome) {
        if (excludeBiomes == null) {
            return true;
        } else {
            return !excludeBiomes.contains(biome);
        }
    }
    public static TakeAimConfig getFromConfig(FileConfiguration config, CustomLogger customLogger) throws InvalidConfigException {
        final boolean enabled = ConfigReaderSimple.getBoolean(config, customLogger, "enabled", "'enabled' flag", true);
        final Set<String> worlds = getStringSet(config, customLogger, "worlds", "worlds");
        final Set<Biome> excludeBiomes = getSet(Biome.class, config, customLogger, "exclude-biomes", "exclude-biomes");

        return new TakeAimConfig(enabled, worlds, excludeBiomes);
    }

    public String toString() {
        return String.format("enabled: %b, worlds: %s, exclude-biomes: %s",
                enabled, worlds, excludeBiomes);
    }
}
