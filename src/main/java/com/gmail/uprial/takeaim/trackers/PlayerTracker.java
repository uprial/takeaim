package com.gmail.uprial.takeaim.trackers;

import com.gmail.uprial.takeaim.TakeAim;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static com.gmail.uprial.takeaim.ballistics.ProjectileMotion.DEFAULT_PLAYER_ACCELERATION;
import static com.gmail.uprial.takeaim.common.Utils.SERVER_TICKS_IN_SECOND;

public class PlayerTracker extends AbstractTracker {
    private static final double EPSILON = 1.0E-5D;

    static class Checkpoint {
        final private Location location;
        final private boolean isJumping;

        Checkpoint(final Location location, final boolean isJumping) {
            this.location = location;
            this.isJumping = isJumping;
        }
    }

    static class TimerWheel extends HashMap<Integer, Checkpoint> {
    }

    /*
        A shorter interval is used to extrapolate the next move.

        WARNING: This fine-tuned number is used to detect jumps
        and predict the next move. You can't just change it. :)
     */
    static final int INTERVAL = SERVER_TICKS_IN_SECOND / 4;
    /*
        A longer interval is used to analyze jump history.

        Must be at least two to proper timer wheel function.
     */
    static final int MAX_HISTORY_LENGTH = 5 * SERVER_TICKS_IN_SECOND / INTERVAL;

    /*
        A player is moving straight
        when its move direction between intervals
        deviates no more than this epsilon.
     */
    private static final double PROPORTION_EPSILON = 0.02D;

    private final TakeAim plugin;

    final Map<UUID, TimerWheel> players = new HashMap<>();
    int currentIndex = 0;

    public PlayerTracker(final TakeAim plugin) {
        super(plugin, INTERVAL);

        this.plugin = plugin;

        onConfigChange();
    }

    public Vector getPlayerMovementVector(final Player player) {
        final UUID uuid = player.getUniqueId();
        final TimerWheel wheel = players.get(uuid);
        if(wheel != null) {
            final Checkpoint current = wheel.get(currentIndex);
            final Checkpoint previous = wheel.get(getPrev(currentIndex));
            if((current != null) && (previous != null)) {
                if(isPlayerJumping(player) || previous.isJumping || current.isJumping) {
                    // Try to predict Y more precise when the player is jumping
                    final Double jumpVy = getAverageVerticalJumpVelocity(player.getLocation(), wheel);
                    if(jumpVy != null) {
                        return new Vector(
                                (current.location.getX() - previous.location.getX()) / INTERVAL,
                                jumpVy,
                                (current.location.getZ() - previous.location.getZ()) / INTERVAL
                        );
                    }
                }

                final Vector lastMove = getDeducted(previous.location, current.location, 1);

                int sameDirectionIntervals = 1;
                Checkpoint sameDirectionStart = null;

                int tmpIndex = getPrev(currentIndex);
                for (int i = 0; i < MAX_HISTORY_LENGTH - 2; i++) {
                    final Checkpoint tmpCurrent = wheel.get(tmpIndex);
                    tmpIndex = getPrev(tmpIndex);
                    final Checkpoint tmpPrevious = wheel.get(tmpIndex);

                    if(tmpPrevious == null) {
                        break;
                    }

                    final Vector tmpMove = getDeducted(tmpPrevious.location, tmpCurrent.location, 1);
                    if(!isProportionalMove(tmpMove, lastMove)) {
                        break;
                    }

                    sameDirectionIntervals ++;
                    sameDirectionStart = tmpPrevious;
                }

                if(sameDirectionStart == null) {
                    return lastMove;
                } else {
                    return getDeducted(sameDirectionStart.location, current.location, sameDirectionIntervals);
                }
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

    private Double getAverageVerticalJumpVelocity(final Location location, final TimerWheel wheel) {
        // All Ys
        List<Double> Ys = new ArrayList<>();
        {
            // Let's start from the next index, which is the last existing record in timerWheel.
            int tmpIndex = getNext(currentIndex);
            // Fetch all the timerWheel in a loop. The last index in the loop will be the current global index.
            for (int i = 0; i < MAX_HISTORY_LENGTH - 1; i++) {
                final Checkpoint checkpoint = wheel.get(tmpIndex);
                /*
                    If the player has just joined the game,
                    it won't have all the records in the timerWheel.

                    The most complicated case is when a player respawns
                    and then logs out and then logs in:
                    there will be a gap in records because the timer wheel index
                    is global for all the players.
                    So, this part of code can't be optimized to
                    while (!wheel.containsKey(...)) i++; while (i < N) Ys.add();
                 */
                if (checkpoint != null) {
                    Ys.add(checkpoint.location.getY());
                }
                tmpIndex = getNext(tmpIndex);
            }
            // We're in a function called before the last player position is stored in the timer wheel
            Ys.add(location.getY());
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
        When the server is under load, e.g., generating new terrain,
        and when the player moves,
        then the server is updating the player location not smoothly,
        but with different delays.

        To increase the precision of the player move prediction,
        I average all the player movements in the same direction.

        And "same direction" means that two moves are proportional.
     */
    static boolean isProportionalMove(final Vector m1, final Vector m2) {
        final double l1 = m1.length();
        final double l2 = m2.length();
        return (l1 >= EPSILON) && (l2 >= EPSILON)
                && (Math.abs(m1.getX() / l1 - m2.getX() / l2) < PROPORTION_EPSILON)
                && (Math.abs(m1.getY() / l1 - m2.getY() / l2) < PROPORTION_EPSILON)
                && (Math.abs(m1.getZ() / l1 - m2.getZ() / l2) < PROPORTION_EPSILON);
    }

    private Vector getDeducted(final Location source, final Location target, final int internals) {
        return new Vector(
                (target.getX() - source.getX()) / INTERVAL / internals,
                (target.getY() - source.getY()) / INTERVAL / internals,
                (target.getZ() - source.getZ()) / INTERVAL / internals
        );
    }

    /*
        An idea of how to detect a jump:
            - the player is not flying, not swimming, now climbing
            - a player vertical velocity does not equal to the default player vertical velocity
     */
    private boolean isPlayerJumping(final Player player) {
        return ((!player.isFlying())
                && (!player.isGliding())
                && (!isPlayerMethod(player, "isSwimming"))
                && (!isPlayerMethod(player, "isClimbing"))
                && (Math.abs(player.getVelocity().getY() - DEFAULT_PLAYER_ACCELERATION) > EPSILON));
    }

    private boolean isPlayerMethod(final Player player, final String name) {
        try {
            return (Boolean)player.getClass().getMethod(name).invoke(player);
        } catch (NoSuchMethodException e) {
            return false;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}