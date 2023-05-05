package com.gmail.uprial.takeaim;

import com.gmail.uprial.takeaim.config.InvalidConfigException;
import com.gmail.uprial.takeaim.helpers.TestConfigBase;
import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class TakeAimConfigTest extends TestConfigBase {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    @Test
    public void testEmptyDebug() throws Exception {
        e.expect(RuntimeException.class);
        e.expectMessage("Empty 'debug' flag. Use default value false");
        TakeAimConfig.isDebugMode(getPreparedConfig(""), getDebugFearingCustomLogger());
    }

    @Test
    public void testNormalDebug() throws Exception {
        assertTrue(TakeAimConfig.isDebugMode(getPreparedConfig("debug: true"), getDebugFearingCustomLogger()));
    }

    @Test
    public void testEmpty() throws Exception {
        e.expect(RuntimeException.class);
        e.expectMessage("Empty 'enabled' flag. Use default value true");
        loadConfig(getDebugFearingCustomLogger(), "");
    }

    @Test
    public void testNotMap() throws Exception {
        e.expect(InvalidConfigurationException.class);
        e.expectMessage("Top level is not a Map.");
        loadConfig("x");
    }

    @Test
    public void testEmptyWorlds() throws Exception {
        assertEquals(
                "enabled: true, worlds: null",
                loadConfig("enabled: true", "").toString());
    }

    @Test
    public void testWorldsListHasDuplicates() throws Exception {
        e.expect(InvalidConfigException.class);
        e.expectMessage("String 'world' in worlds is not unique");
        loadConfig("enabled: true",
                "worlds:",
                " - world",
                " - world");
    }

    @Test
    public void testNormalConfig() throws Exception {
        assertEquals(
                "enabled: true, worlds: [world]",
                loadConfig("enabled: true",
                        "worlds:",
                        " - world").toString());
    }
}