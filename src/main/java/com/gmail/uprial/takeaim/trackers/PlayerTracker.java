package com.gmail.uprial.takeaim.trackers;

import com.gmail.uprial.takeaim.TakeAim;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.gmail.uprial.takeaim.ballistics.ProjectileMotion.PLAYER_ACCELERATION;
import static com.gmail.uprial.takeaim.common.Utils.SERVER_TICKS_IN_SECOND;

public class PlayerTracker extends AbstractTracker {
    private static final double epsilon = 1.0E-6D;

    private class Checkpoint {
        final private Location location;
        final private Boolean isJumping;

        Checkpoint(final Location location, final Boolean isJumping) {
            this.location = location;
            this.isJumping = isJumping;
        }
    }

    private class TimerWheel extends HashMap<Integer, Checkpoint> {
    }

    private static final int INTERVAL = SERVER_TICKS_IN_SECOND / 4;
    private static final int MAX_HISTORY_LENGTH = 5 * SERVER_TICKS_IN_SECOND / INTERVAL;

    private final TakeAim plugin;

    private final Map<UUID, TimerWheel> players = new HashMap<>();
    private int index = 0;

    public PlayerTracker(final TakeAim plugin) {
        super(plugin, INTERVAL);

        this.plugin = plugin;

        onConfigChange();
    }

    public Vector getPlayerMovementVector(final Player player) {
        final UUID uuid = player.getUniqueId();
        final TimerWheel wheel = players.get(uuid);
        if(wheel != null) {
            final Checkpoint current = wheel.get(index);
            final Checkpoint previous = wheel.get(getPrev(index));
            if((current != null) && (previous != null)) {
                final double vy;
                if(isPlayerJumping(player) || previous.isJumping || current.isJumping) {
                    vy = getAverageVerticalJumpVelocity(wheel);
                } else {
                    vy = (current.location.getY() - previous.location.getY()) / INTERVAL;
                }
                return new Vector(
                        (current.location.getX() - previous.location.getX()) / INTERVAL,
                        vy,
                        (current.location.getZ() - previous.location.getZ()) / INTERVAL
                );
            }
        }
        return new Vector(0.0, 0.0, 0.0);
    }

    public Player getOnlinePlayerByUUID(final UUID uuid) {
        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getUniqueId().equals(uuid)) {
                return player;
            }
        }

        return null;
    }

    @Override
    public void run() {
        index = getNext(index);
        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            final UUID uuid = player.getUniqueId();
            TimerWheel wheel = players.get(uuid);

            if(player.isDead()) {
                if (wheel != null) {
                    players.remove(uuid);
                }
            } else {
                if (wheel == null) {
                    wheel = new TimerWheel();
                    players.put(uuid, wheel);
                }
                wheel.put(index, new Checkpoint(player.getLocation(), isPlayerJumping(player)));
            }
            // System.out.println(String.format("Velocity: %s, Jumping: %b", format(player.getVelocity()), isPlayerJumping(player)));
        }
    }

    @Override
    protected void clear() {
        players.clear();
    }

    @Override
    protected boolean isEnabled() {
        return plugin.getTakeAimConfig().isEnabled();
    }

    private int getNext(int index) {
        index ++;
        if(index >= MAX_HISTORY_LENGTH) {
            index = 0;
        }

        return index;
    }

    private int getPrev(int index) {
        index --;
        if(index < 0) {
            index = MAX_HISTORY_LENGTH - 1;
        }

        return index;
    }

    private double getAverageVerticalJumpVelocity(final TimerWheel wheel) {
        final double vy;

        // Let's start from the next index, which is the last existing record in timerWheel.
        Double firstY = null;
        Double lastY = null;

        int tmpIndex = getNext(index);
        Double y1 = null;
        Double y2 = null;
        // Fetch all the timerWheel in a loop. The last index in the loop will be the current global index.
        for(int i = 0; i < MAX_HISTORY_LENGTH - 1; i++) {
            final Checkpoint checkpoint = wheel.get(tmpIndex);
            // If the player has just joined the game, it won't have all the records in the timerWheel.
            if(checkpoint != null) {
                // There is a sequence of 3 coordinates: y2 -> y1 -> y0.
                double y0 = checkpoint.location.getY();
                // Check that we have enough records in the timerWheel.
                if(y2 != null) {
                    // Let's find an extremum where y1 is lower than both y2 and y0.
                    if((y2 > y1) && (y1 > y0)) {
                        if(firstY == null) {
                            firstY = y1;
                        } else {
                            lastY = y1;
                        }
                    }
                }

                y2 = y1;
                y1 = y0;
            }
            tmpIndex = getNext(tmpIndex);
        }

        // We have two different extremums, project the next move.
        if((firstY != null) && (lastY != null)) {
            vy = (lastY - firstY) / (MAX_HISTORY_LENGTH * INTERVAL) + PLAYER_ACCELERATION;
        // We have only one extremum, we're jumping for sure but have no idea how high.
        } else if (firstY != null) {
            vy = PLAYER_ACCELERATION;
        // Something goes wrong, and we don't have extremums. Let's not predict the vertical move.
        } else {
            vy = 0.0D;
        }
        // System.out.println(String.format("vy: %.4f, firstY: %.4f, lastY: %.4f", vy, firstY, lastY));

        return vy;
    }

    /*
        An idea of how to detect a jump:
            - the player is not flying
            - the player is not on the ladder
            - a player vertical velocity does not equal to the default player vertical velocity
     */
    private boolean isPlayerJumping(final Player player) {
        return ((!player.isFlying())
                && (!player.getLocation().getBlock().getType().equals(Material.LADDER))
                && (Math.abs(player.getVelocity().getY() - PLAYER_ACCELERATION) > epsilon));
    }
}