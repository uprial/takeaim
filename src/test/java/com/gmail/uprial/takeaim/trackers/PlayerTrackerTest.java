package com.gmail.uprial.takeaim.trackers;

import com.gmail.uprial.takeaim.helpers.TestConfigBase;
import org.bukkit.util.Vector;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

public class PlayerTrackerTest extends TestConfigBase {

    @Test
    public void testGetAverageVerticalJumpVelocity_NoJump() {
        for (List<Double> Ys : new ArrayList<List<Double>>() {{
            add(Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.5));
            add(Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0, 1.5, 2.0, 1.2));
            add(Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.5, 2.0, 1.2, 1.0));
            add(Arrays.asList(1.0, 1.0, 1.0, 1.5, 2.0, 1.2, 1.0, 1.3));
            add(Arrays.asList(1.0, 1.0, 1.5, 2.0, 1.2, 1.0, 1.3, 2.0));
        }}) {
            assertNull(PlayerTracker.getAverageVerticalJumpVelocity(Ys));
        }
    }

    @Test
    public void testGetAverageVerticalJumpVelocity_Horizontal() {
        //noinspection ConstantConditions
        assertEquals(0.00, PlayerTracker.getAverageVerticalJumpVelocity(
                Arrays.asList(1.0, 1.5, 2.0, 1.2, 1.0, 1.3, 2.0, 1.1)), 0.0001D);
    }

    @Test
    public void testGetAverageVerticalJumpVelocity_Up() {
        //noinspection ConstantConditions
        assertEquals(+0.01, PlayerTracker.getAverageVerticalJumpVelocity(
                Arrays.asList(1.0, 1.5, 2.0, 1.2, 1.0, 1.3, 2.2, 1.1)), 0.0001D);
    }

    @Test
    public void testGetAverageVerticalJumpVelocity_Down() {
        //noinspection ConstantConditions
        assertEquals(-0.01, PlayerTracker.getAverageVerticalJumpVelocity(
                Arrays.asList(1.0, 1.5, 2.0, 1.2, 1.0, 1.3, 1.8, 1.1)), 0.0001D);
    }

    @Test
    public void testGetAverageVerticalJumpVelocity_UpWithNull() {
        //noinspection ConstantConditions
        assertEquals(+0.0133, PlayerTracker.getAverageVerticalJumpVelocity(
                Arrays.asList(1.0, 1.5, 2.0, null, 1.0, 1.3, 2.2, 1.1)), 0.0001D);
        //noinspection ConstantConditions
        assertEquals(+0.0133, PlayerTracker.getAverageVerticalJumpVelocity(
                Arrays.asList(1.0, 1.5, 2.0, null, 1.0, null, 2.2, 1.1)), 0.0001D);
    }
}