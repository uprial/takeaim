package com.gmail.uprial.takeaim.config;

import com.gmail.uprial.takeaim.helpers.TestConfigBase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Set;

import static com.gmail.uprial.takeaim.config.ConfigReaderEnums.getStringSet;
import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

public class ConfigReaderEnumsTest extends TestConfigBase {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    // ==== getStringSet ====

    @Test
    public void testEmptyStringSet() throws Exception {
        e.expect(RuntimeException.class);
        e.expectMessage("Empty set. Use default value NULL");
        getStringSet(getPreparedConfig(""), getDebugFearingCustomLogger(), "s", "set");
    }

    @Test
    public void testEmptyStringSetValue() throws Exception {
        assertNull(getStringSet(getPreparedConfig(""), getCustomLogger(), "s", "set"));
    }

    @Test
    public void testNotUniqueStringSet() throws Exception {
        e.expect(InvalidConfigException.class);
        e.expectMessage("String 'A' in set is not unique");
        getStringSet(getPreparedConfig("x:", "  entities:", "   - A", "   - A"),
                getParanoiacCustomLogger(), "x.entities", "set");
    }

    @Test
    public void testNormalStringSet() throws Exception {
        Set<String> set = getStringSet(getPreparedConfig("entities:", " - A"),
                getParanoiacCustomLogger(), "entities", "path");
        assertNotNull(set);
        assertEquals("[A]", set.toString());
    }

    @Test
    public void testContentOfStringSet() throws Exception {
        Set<String> entities = getStringSet(getPreparedConfig("entities:", " - A", " - B"),
                getParanoiacCustomLogger(), "entities", "path");
        assertNotNull(entities);
        assertEquals(2, entities.size());
        assertTrue(entities.contains("A"));
        assertTrue(entities.contains("B"));
    }
}