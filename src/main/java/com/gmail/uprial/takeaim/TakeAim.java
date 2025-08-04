package com.gmail.uprial.takeaim;

import com.gmail.uprial.takeaim.common.CustomLogger;
import com.gmail.uprial.takeaim.config.InvalidConfigException;
import com.gmail.uprial.takeaim.listeners.TakeAimAttackEventListener;
import com.gmail.uprial.takeaim.trackers.PlayerTracker;
import com.gmail.uprial.takeaim.trackers.ProjectileTracker;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

import static com.gmail.uprial.takeaim.TakeAimCommandExecutor.COMMAND_NS;

public final class TakeAim extends JavaPlugin {
    private final String CONFIG_FILE_NAME = "config.yml";
    private final File configFile = new File(getDataFolder(), CONFIG_FILE_NAME);

    private CustomLogger consoleLogger = null;
    private TakeAimConfig takeAimConfig = null;

    private PlayerTracker playerTracker;
    private ProjectileTracker projectileTracker;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        consoleLogger = new CustomLogger(getLogger());
        takeAimConfig = loadConfig(getConfig(), consoleLogger);

        playerTracker = new PlayerTracker(this, consoleLogger, takeAimConfig.getPlayerTrackingTimeoutInMs());
        projectileTracker = new ProjectileTracker(this, consoleLogger);

        getServer().getPluginManager().registerEvents(new TakeAimAttackEventListener(this, consoleLogger), this);

        getCommand(COMMAND_NS).setExecutor(new TakeAimCommandExecutor(this));
        consoleLogger.info("Plugin enabled");
    }

    public TakeAimConfig getTakeAimConfig() {
        return takeAimConfig;
    }

    public PlayerTracker getPlayerTracker() {
        return playerTracker;
    }

    public ProjectileTracker getProjectileTracker() {
        return projectileTracker;
    }

    void reloadConfig(CustomLogger userLogger) {
        reloadConfig();
        takeAimConfig = loadConfig(getConfig(), userLogger, consoleLogger);
        playerTracker.onConfigChange();
        projectileTracker.onConfigChange();
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        projectileTracker.stop();
        playerTracker.stop();
        consoleLogger.info("Plugin disabled");
    }

    @Override
    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            saveResource(CONFIG_FILE_NAME, false);
        }
    }

    @Override
    public FileConfiguration getConfig() {
        return YamlConfiguration.loadConfiguration(configFile);
    }

    static TakeAimConfig loadConfig(FileConfiguration config, CustomLogger customLogger) {
        return loadConfig(config, customLogger, null);
    }

    private static TakeAimConfig loadConfig(FileConfiguration config, CustomLogger mainLogger, CustomLogger secondLogger) {
        TakeAimConfig takeAimConfig = null;
        try {
            boolean isDebugMode = TakeAimConfig.isDebugMode(config, mainLogger);
            mainLogger.setDebugMode(isDebugMode);
            if(secondLogger != null) {
                secondLogger.setDebugMode(isDebugMode);
            }

            takeAimConfig = TakeAimConfig.getFromConfig(config, mainLogger);
        } catch (InvalidConfigException e) {
            mainLogger.error(e.getMessage());
        }

        return takeAimConfig;
    }
}
