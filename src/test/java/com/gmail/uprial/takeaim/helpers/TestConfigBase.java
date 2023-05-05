package com.gmail.uprial.takeaim.helpers;

import com.gmail.uprial.takeaim.TakeAimConfig;
import com.gmail.uprial.takeaim.config.InvalidConfigException;
import com.google.common.collect.Lists;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import static com.gmail.uprial.takeaim.common.Utils.joinStrings;

@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public abstract class TestConfigBase {
    protected static void loadConfig(String content) throws InvalidConfigurationException, InvalidConfigException {
        loadConfig(new String[]{content});
    }

    protected static TakeAimConfig loadConfig(String... contents) throws InvalidConfigurationException, InvalidConfigException {
        return loadConfig(getCustomLogger(), contents);
    }

    protected static TakeAimConfig loadConfig(TestCustomLogger testCustomLogger, String... contents) throws InvalidConfigurationException, InvalidConfigException {
        return TakeAimConfig.getFromConfig(getPreparedConfig(contents), testCustomLogger);
    }

    protected static YamlConfiguration getPreparedConfig(String... contents) throws InvalidConfigurationException {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        yamlConfiguration.loadFromString(joinStrings(System.lineSeparator(), Lists.newArrayList(contents)));

        return yamlConfiguration;
    }

    protected static TestCustomLogger getCustomLogger() {
        return new TestCustomLogger();
    }


    protected static TestCustomLogger getDebugFearingCustomLogger() {
        TestCustomLogger testCustomLogger = new TestCustomLogger();
        testCustomLogger.doFailOnDebug();

        return testCustomLogger;
    }

    protected static TestCustomLogger getParanoiacCustomLogger() {
        TestCustomLogger testCustomLogger = new TestCustomLogger();
        testCustomLogger.doFailOnAny();

        return testCustomLogger;
    }

    protected static TestCustomLogger getIndifferentCustomLogger() {
        TestCustomLogger testCustomLogger = new TestCustomLogger();
        testCustomLogger.doNotFailOnError();

        return testCustomLogger;
    }
}
