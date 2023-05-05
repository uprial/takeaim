package com.gmail.uprial.takeaim.trackers;

import com.gmail.uprial.takeaim.TakeAim;
import com.gmail.uprial.takeaim.common.CustomLogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

import static com.gmail.uprial.takeaim.ballistics.ProjectileMotion.DEFAULT_PLAYER_ACCELERATION;
import static com.gmail.uprial.takeaim.common.Formatter.format;
import static com.gmail.uprial.takeaim.common.Utils.SERVER_TICKS_IN_SECOND;

public class PlayerTracker extends AbstractTracker {
    private static final double epsilon = 1.0E-5D;

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

    /*
        A shorter interval is used to extrapolate the next move.

        WARNING: This fine-tuned number is used to detect jumps
        and predict the next move. You can't just change it. :)
     */
    private static final int INTERVAL = SERVER_TICKS_IN_SECOND / 4;
    /*
        A longer interval is used to analyze jump history.

        Must be at least two to proper timer wheel function.
     */
    private static final int MAX_HISTORY_LENGTH = 5 * SERVER_TICKS_IN_SECOND / INTERVAL;

    private final TakeAim plugin;
    private final CustomLogger customLogger;

    private final Map<UUID, TimerWheel> players = new HashMap<>();
    private int currentIndex = 0;

    public PlayerTracker(final TakeAim plugin, final CustomLogger customLogger) {
        super(plugin, INTERVAL);

        this.plugin = plugin;
        this.customLogger = customLogger;

        onConfigChange();
    }

    public Vector getPlayerMovementVector(final Player player) {
        final UUID uuid = player.getUniqueId();
        final TimerWheel wheel = players.get(uuid);
        if(wheel != null) {
            final Checkpoint current = wheel.get(currentIndex);
            final Checkpoint previous = wheel.get(getPrev(currentIndex));
            if((current != null) && (previous != null)) {
                final double vy;
                if(isPlayerJumping(player) || previous.isJumping || current.isJumping) {
                    final Double jumpVy = getAverageVerticalJumpVelocity(player, wheel);
                    if(jumpVy != null) {
                        vy = jumpVy;
                    } else {
                        vy = (player.getLocation().getY() - current.location.getY()) / INTERVAL;
                    }
                    // System.out.println(String.format("jumpVy: %.4f", jumpVy));
                } else {
                    vy = (current.location.getY() - previous.location.getY()) / INTERVAL;
                }
                // System.out.println(String.format("vy: %.4f", vy));
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
        final int nextIndex = getNext(currentIndex);
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
                wheel.put(nextIndex, new Checkpoint(player.getLocation(), isPlayerJumping(player)));
            }
            // System.out.println(String.format("Velocity: %s, Jumping: %b", format(player.getVelocity()), isPlayerJumping(player)));
        }
        currentIndex = nextIndex;
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

    private Double getAverageVerticalJumpVelocity(final Player player, final TimerWheel wheel) {
        // All Ys
        List<Double> Ys = new ArrayList<>();
        {
            // Let's start from the next index, which is the last existing record in timerWheel.
            int tmpIndex = getNext(currentIndex);
            // Fetch all the timerWheel in a loop. The last index in the loop will be the current global index.
            for (int i = 0; i < MAX_HISTORY_LENGTH - 1; i++) {
                final Checkpoint checkpoint = wheel.get(tmpIndex);
                // If the player has just joined the game, it won't have all the records in the timerWheel.
                if (checkpoint != null) {
                    Ys.add(checkpoint.location.getY());
                }
                tmpIndex = getNext(tmpIndex);
            }
            // We're in a function called before the last player position is stored in the timer wheel
            Ys.add(player.getLocation().getY());
        }

        return getAverageVerticalJumpVelocity(Ys);
    }

    /*
        Returns a projection of the next move based on two jump extremums
        or returns null.
     */
    static Double getAverageVerticalJumpVelocity(final List<Double> Ys) {
        // Y of first and last jumps detected
        Double firstExtremumY = null;
        int firstExtremumI = -1;
        Double lastExtremumY = null;
        int lastExtremumI = -1;
        // There is a sequence of 3 coordinates: y2 -> y1 -> y0.
        Double y1 = null;
        Double y2 = null;
        // Fetch all the timerWheel in a loop. The last index in the loop will be the current global index.
        int i = 0;
        for (Double y0 : Ys) {
            if(y0 != null) {
                // Check that we have enough records in the timerWheel.
                if (y2 != null) {
                    // Let's find an extremum where y1 is higher than both y2 and y0.
                    if ((y0 < y1) && (y1 > y2)) {
                        if (firstExtremumY == null) {
                            // Remember 1st extremum
                            firstExtremumY = y1;
                            firstExtremumI = i;
                        } else {
                            // If there ist the second extremum - we found a finishing 2nd jump!
                            lastExtremumY = y1;
                            lastExtremumI = i;
                        }
                    }
                }

                y2 = y1;
                y1 = y0;
            }

            i++;
        }

        if((firstExtremumY != null) && (lastExtremumY != null)) {
            /*
                We have two different extremums, let's project the next move
                based on the 1st and the 2nd extremums.
             */
            return (lastExtremumY - firstExtremumY) / (lastExtremumI - firstExtremumI) / INTERVAL;
        } else {
            return null;
        }
    }

    /*
        An idea of how to detect a jump:
            - the player is not flying, not swimming, now climbing
            - a player vertical velocity does not equal to the default player vertical velocity
     */
    private boolean isPlayerJumping(final Player player) {
        return ((!player.isFlying())
                && (!player.isSwimming())
                && (!player.isClimbing())
                && (Math.abs(player.getVelocity().getY() - DEFAULT_PLAYER_ACCELERATION) > epsilon));
    }
}