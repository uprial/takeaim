package com.gmail.uprial.takeaim.trackers;

import com.gmail.uprial.takeaim.TakeAim;
import com.gmail.uprial.takeaim.helpers.TestConfigBase;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.gmail.uprial.takeaim.ballistics.ProjectileMotion.DEFAULT_PLAYER_ACCELERATION;
import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class PlayerTrackerTest extends TestConfigBase {
    private static class TestCheckPoint extends PlayerTracker.Checkpoint {
        TestCheckPoint(final double x, final double y, final double z) {
            super(new Location(null, x, y, z), false);
        }
    }

    private static class TestPlayerTracker extends PlayerTracker {
        TestPlayerTracker(final TakeAim plugin, final Player player) {
            super(plugin, null, 0);

            players.put(player.getUniqueId(), new PlayerTracker.TimerWheel());
        }

        @Override
        public void onConfigChange() {
        }

        void setIndex(final int index) {
            currentIndex = index;
        }

        PlayerTracker.TimerWheel getPlayerWheel(final Player player) {
            return players.get(player.getUniqueId());
        }
    }

    private TestPlayerTracker playerTracker;
    private PlayerTracker.TimerWheel wheel;
    private Player player;

    @Before
    public void setUp() {
        final UUID playerId = UUID.randomUUID();

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getVelocity()).thenReturn(new Vector(0, DEFAULT_PLAYER_ACCELERATION, 0));

        playerTracker = new TestPlayerTracker(null, player);
        wheel = playerTracker.getPlayerWheel(player);
    }

    @After
    public void tearDown() {
    }

    // ==== getAverageVerticalJumpVelocity ====

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

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testGetAverageVerticalJumpVelocity_UpWithNull() {
        assertEquals(+0.0133, PlayerTracker.getAverageVerticalJumpVelocity(
                Arrays.asList(1.0, 1.5, 2.0, null, 1.0, 1.3, 2.2, 1.1)), 0.0001D);
        assertEquals(+0.0133, PlayerTracker.getAverageVerticalJumpVelocity(
                Arrays.asList(1.0, 1.5, 2.0, null, 1.0, null, 2.2, 1.1)), 0.0001D);
        assertEquals(+0.0133, PlayerTracker.getAverageVerticalJumpVelocity(
                Arrays.asList(null, 1.5, 2.0, null, 1.0, null, 2.2, 1.1)), 0.0001D);

        assertNull(PlayerTracker.getAverageVerticalJumpVelocity(
                Arrays.asList(null, null, 2.0, null, 1.0, null, 2.2, 1.1)));
    }

    // ==== isProportionalMove ====

    @Test
    public void testIsProportionalMove_Staying() {
        assertFalse(PlayerTracker.isProportionalMove(
                new Vector(1, 0, 0), new Vector(0, 0, 0)));
        assertFalse(PlayerTracker.isProportionalMove(
                new Vector(0, 0, 0), new Vector(1, 0, 0)));
    }

    @Test
    public void testIsProportionalMove_Moving() {
        assertTrue(PlayerTracker.isProportionalMove(
                new Vector(1, 0, 0), new Vector(1, 0, 0)));
        assertTrue(PlayerTracker.isProportionalMove(
                new Vector(1, 0, 0), new Vector(2, 0, 0)));
        assertTrue(PlayerTracker.isProportionalMove(
                new Vector(1, 0, 2), new Vector(2, 0, 4)));

        assertTrue(PlayerTracker.isProportionalMove(
                new Vector(1.4750, 0, 1.2905), new Vector(1.4751, 0, 1.2906)));
        assertTrue(PlayerTracker.isProportionalMove(
                new Vector(1.4751, 0, 1.2906), new Vector(0.1639, 0, 0.1434)));
        assertTrue(PlayerTracker.isProportionalMove(
                new Vector(0.1639, 0, 0.1434), new Vector(0.2459, 0, 0.2151)));
        assertTrue(PlayerTracker.isProportionalMove(
                new Vector(0.2459, 0, 0.2151), new Vector(0.7376, 0, 0.6453)));
        assertTrue(PlayerTracker.isProportionalMove(
                new Vector(0.7376, 0, 0.6453), new Vector(0.1639, 0, 0.1434)));
        assertTrue(PlayerTracker.isProportionalMove(
                new Vector(0.1639, 0, 0.1434), new Vector(0.3278, 0, 0.2868)));
    }

    // ==== getPlayerMovementVector ====

    @Test
    public void testGetPlayerMovementVector_NoData() {
        // No move data
        assertEquals(new Vector(0, 0, 0),
                playerTracker.getPlayerMovementVector(player));
    }

    @Test
    public void testGetPlayerMovementVector_OnePoint() {
        wheel.put(0, new TestCheckPoint(1.0D, 0, 0));
        assertEquals(new Vector(0, 0, 0),
                playerTracker.getPlayerMovementVector(player));
    }

    @Test
    public void testGetPlayerMovementVector_TwoPoints() {
        wheel.put(0, new TestCheckPoint(1.0D, 0, 0));
        wheel.put(1, new TestCheckPoint(2.5D, 0, 0.1D));
        playerTracker.setIndex(1);

        assertEquals(new Vector(1.5D / PlayerTracker.INTERVAL, 0, 0.1D / PlayerTracker.INTERVAL),
                playerTracker.getPlayerMovementVector(player));
    }

    @Test
    public void testGetPlayerMovementVector_Jump() {
        // Not default vertical velocity triggers isPlayerJumping()
        when(player.getVelocity()).thenReturn(new Vector(0, 0, 0));

        int index = 0;
        for(double y : Arrays.asList(1.0, 1.5, 2.0, 1.2, 1.0, 1.3, 2.2, 1.1)) {
            wheel.put(index, new TestCheckPoint(0, y, 0));
            index++;
        }
        playerTracker.setIndex(index - 1);

        // Put the last move in the current location
        when(player.getLocation()).thenReturn(new Location(null,0, 1.0, 0));

        assertEquals(new Vector(0, +0.01, 0),
                playerTracker.getPlayerMovementVector(player));
    }

    @Test
    public void testGetPlayerMovementVector_WrongProportion() {
        wheel.put(0, new TestCheckPoint(1.0D, 0, 0));
        wheel.put(1, new TestCheckPoint(2.5D, 0, 0));
        wheel.put(2, new TestCheckPoint(5.5D, 0, 1.0D));
        playerTracker.setIndex(2);

        assertEquals(new Vector(3.0D / PlayerTracker.INTERVAL, 0, 1.0D / PlayerTracker.INTERVAL),
                playerTracker.getPlayerMovementVector(player));
    }

    @Test
    public void testGetPlayerMovementVector_Proportion_3() {
        wheel.put(0, new TestCheckPoint(1.0D, 0, 0));
        wheel.put(1, new TestCheckPoint(2.5D, 0, 0));
        wheel.put(2, new TestCheckPoint(5.5D, 0, 0));
        playerTracker.setIndex(2);

        assertEquals(new Vector(4.5D / PlayerTracker.INTERVAL / 2, 0, 0),
                playerTracker.getPlayerMovementVector(player));
    }

    @Test
    public void testGetPlayerMovementVector_Proportion_3_InEpsilon() {
        wheel.put(0, new TestCheckPoint(1.0D, 0, 0));
        wheel.put(1, new TestCheckPoint(2.5D, 0, 0));
        wheel.put(2, new TestCheckPoint(5.5D, 0, 0.001D));
        playerTracker.setIndex(2);

        assertEquals(new Vector(4.5D / PlayerTracker.INTERVAL / 2, 0, 0.001D / PlayerTracker.INTERVAL / 2),
                playerTracker.getPlayerMovementVector(player));
    }

    @Test
    public void testGetPlayerMovementVector_Proportion_3_OutEpsilon() {
        wheel.put(0, new TestCheckPoint(1.0D, 0, 0));
        wheel.put(1, new TestCheckPoint(2.5D, 0, 0));
        wheel.put(2, new TestCheckPoint(5.5D, 0, 0.08D));
        playerTracker.setIndex(2);

        assertEquals(new Vector(3.0D / PlayerTracker.INTERVAL, 0, 0.08D / PlayerTracker.INTERVAL),
                playerTracker.getPlayerMovementVector(player));
    }

    @Test
    public void testGetPlayerMovementVector_Proportion_AllPoints() {
        wheel.put(0, new TestCheckPoint(1.0D, 0, 0));
        wheel.put(1, new TestCheckPoint(2.5D, 0, 0));

        final int maxIndex = PlayerTracker.MAX_HISTORY_LENGTH - 1;

        for (int i = 2; i <= maxIndex; i++) {
            playerTracker.setIndex(i);
            wheel.put(i, new TestCheckPoint(5.5D + i, 0, 0));
            assertEquals(new Vector((4.5D + i) / PlayerTracker.INTERVAL / i, 0, 0),
                    playerTracker.getPlayerMovementVector(player));
        }

        playerTracker.setIndex(maxIndex);
        wheel.put(maxIndex, new TestCheckPoint(100.0D, 0, 0));
        assertEquals(new Vector(99.D / PlayerTracker.INTERVAL / maxIndex, 0, 0),
                playerTracker.getPlayerMovementVector(player));
    }

    @Test
    public void testGetPlayerMovementVector_Proportion_TooManyPoints() {
        wheel.put(0, new TestCheckPoint(1.0D, 0, 0));
        wheel.put(1, new TestCheckPoint(2.5D, 0, 0));

        final int maxIndex = PlayerTracker.MAX_HISTORY_LENGTH;

        for (int i = 2; i <= maxIndex; i++) {
            wheel.put(i, new TestCheckPoint(5.5D + i, 0, 0));
        }
        playerTracker.setIndex(maxIndex);
        // Starts from 2.5D instead of 0D
        assertEquals(new Vector((3.0D + maxIndex) / PlayerTracker.INTERVAL / (maxIndex - 1), 0, 0),
                playerTracker.getPlayerMovementVector(player));
    }
}