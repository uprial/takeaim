package com.gmail.uprial.takeaim;

import com.gmail.uprial.takeaim.helpers.TestConfigBase;
import org.junit.Test;

public class TakeAimTest extends TestConfigBase {
    @Test
    public void testLoadException() throws Exception {
        TakeAim.loadConfig(getPreparedConfig(""), getCustomLogger());
    }
}