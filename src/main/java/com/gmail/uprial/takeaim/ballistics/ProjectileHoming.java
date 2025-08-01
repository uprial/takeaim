package com.gmail.uprial.takeaim.ballistics;

import com.gmail.uprial.takeaim.TakeAim;
import com.gmail.uprial.takeaim.common.CustomLogger;
import com.gmail.uprial.takeaim.fixtures.FireballAdapter;
import com.gmail.uprial.takeaim.fixtures.FireballAdapterNotSupported;
import org.bukkit.Location;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

import static com.gmail.uprial.takeaim.common.Formatter.format;
import static com.gmail.uprial.takeaim.common.Utils.SERVER_TICKS_IN_SECOND;

public class ProjectileHoming {
    private final TakeAim plugin;
    private final CustomLogger customLogger;

    private Boolean isFireballAdapterSupported = null;

    private static final double ARROW_TERMINAL_VELOCITY = 100.0D / SERVER_TICKS_IN_SECOND;

    private static final double DRAG_APPROXIMATION_EPSILON = 0.04D;

    private static final int ARROW_DRAG_APPROXIMATION_ATTEMPTS = 5;
    private static final int FIREBALL_DRAG_APPROXIMATION_ATTEMPTS = 10;

    public ProjectileHoming(final TakeAim plugin, final CustomLogger customLogger) {
        this.plugin = plugin;
        this.customLogger = customLogger;
    }

    public boolean hasProjectileMotion(final Projectile projectile) {
        return ProjectileMotion.getProjectileMotion(projectile) != null;
    }

    public void aimProjectile(final LivingEntity projectileSource, final Projectile projectile, final Player targetPlayer) {
        if (projectile instanceof Fireball) {
            if(isFireballAdapterSupported == null || isFireballAdapterSupported) {
                try {
                    aimFireball(projectileSource, (Fireball) projectile, targetPlayer);
                    isFireballAdapterSupported = true;
                } catch (FireballAdapterNotSupported e) {
                    customLogger.warning(String.format("Can't modify projectile velocity of %s targeted at %s: minecraft version not supported",
                            format(projectileSource), format(targetPlayer)));
                    isFireballAdapterSupported = false;
                }
            }
        } else {
            aimArrow(projectileSource, projectile, targetPlayer);
        }
    }

    /*
        Try #1

        Let's assume, we have a flying projectile.
        We should keep its momentum, but retarget better.
        We should assume the target moves.

        s = source (projectile)
        t = target (player)
        e = end point or "meeting" point

        a) The projectile in the end point
        xe = xs + vxs * t
        ye = ys + vys * t + g * t^2 / 2
        ze = zs + vzs * t

        Momentum
        vxs^2 + vys^2 + vzs^2 = r

        The target in the end point
        xe = xt + vxt * t
        ye = yt + vyt * t
        ze = zt + vzt * t

        b) Then
        xt + vxt * t = xs + vxs * t
        yt + vyt * t = ys + vys * t + g * t^2 / 2
        zt + vzt * t = zs + vzs * t
        vxs^2 + vys^2 + vzs^2 = r

        c) Then
        vxs = (xt + vxt * t - xs) / t = vxt + (xt - xs) / t
        vys = (yt + vyt * t - ys) / t - (g * t^2 / 2) / t = vyt + (yt - ys) / t - g * t / 2
        vzs = (zt + vzt * t - zs) / t = vzt + (zt - zs) / t
        vxs^2 + vys^2 + vzs^2 = r

        So, we have a biquadratic equation that only a mother could love.
        Now we should consider environmental drag somehow which increases the degree.
        But 4th is the highest degree that can be solved by radicals.
        Let's try to simplify the case.

        Try #2

        Assume, we don't need to keep the momentum because we control a non-player entity anyway.
        We have to make sure three things happen:
        - a projectile does not fly too fast,
        - the projectile meets the target exactly at the time,
        - the environmental drag is considered.

        So, we need to calculate only Y axis:

        y2 = y1 + vy * t + g * t^2 / 2 =>
            vy = (y2 - g * t^2 / 2 - y1) / t

        g = GRAVITY_ACCELERATION (m/s^2)
        t = ticksInFly (ticks) / t2s (ticks/s) = (s)

        Example:
          y1 = 0
          y2 = 0
          ticksInFly = 10.0

          t = 10.0 / 20.0 = 0.5,
          vy = (0 - (- 20.0 * 0.5^2 / 2) - 0) / 0.5 =:= 20.0 / 4 =:= 5 (m/s)


        Let's consider the environmental drag.

        Sn = b1 * (1 - q^n) / (1 - q) =>
            b1 = Sn * (1 - q) / (1 - q^n)

        Now we have to check that our target isn't too far away.
        Assuming an experimental maximum momentum and an angle of 45 grades, let's calculate a maximum distance.

        m = MAX_ARROW_SPEED_PER_TICK
        t = cos(a) * m / g = sqrt(2) * m / g
        d = sin(a) * m * t = sqrt(2) * sqrt(2) * m * m / g = m^2 / g

     */
    private void aimArrow(final LivingEntity projectileSource, final Projectile projectile, final Player targetPlayer) {
        final Location targetLocation = getAimPoint(targetPlayer);
        // Normalize considering the projectile initial location.
        targetLocation.subtract(projectile.getLocation());

        final Vector initialProjectileVelocity = projectile.getVelocity();

        // How long the projectile will be enforced to fly to the target player.
        final double ticksInFly = targetLocation.length() / initialProjectileVelocity.length();

        // Consider the target player is running somewhere.
        final Vector velocity = plugin.getPlayerTracker().getPlayerMovementVector(targetPlayer);
        targetLocation.add(velocity.clone().multiply(ticksInFly));

        final ProjectileMotion motion = ProjectileMotion.getProjectileMotion(projectile);

        if(motion.hasGravityAcceleration()) {
            final double maxHeight = -Math.pow(ARROW_TERMINAL_VELOCITY, 2.0D) / motion.getGravityAcceleration();

            if (targetLocation.getY() > maxHeight) {
                customLogger.warning(String.format(
                        "Can't modify velocity %s of %s" +
                                " to a height of %.2f in %.2fs: max height is %.2f",
                        format(initialProjectileVelocity), getDescription(projectileSource, projectile, targetPlayer, velocity),
                        targetLocation.getY(), ticksInFly, maxHeight));
                return;
            }
        }

        final Vector newVelocity;
        {
            final double vx = targetLocation.getX() / ticksInFly;
            final double vz = targetLocation.getZ() / ticksInFly;

            final double vy;

            if (motion.hasGravityAcceleration()) {
                final double y1 = 0.0D;
                final double y2 = targetLocation.getY();
                final double t = ticksInFly;

                vy = ((y2 - (motion.getGravityAcceleration() * t * t / 2.0D) - y1) / t);
            } else {
                vy = targetLocation.getY() / ticksInFly;
            }

            newVelocity = new Vector(vx, vy, vz);
        }

        // Consider environmental drag.
        if(motion.hasDrag()) {
            final double q = (1.0D - motion.getDrag());
            final double normalizedDistanceWithDrag = (1.0D - Math.pow(q, ticksInFly)) / (1.0D - q);
            final double dragFix = ticksInFly / normalizedDistanceWithDrag;

            if(motion.hasGravityAcceleration()) {
                newVelocity.setX(newVelocity.getX() * dragFix);
                newVelocity.setZ(newVelocity.getZ() * dragFix);

                /*
                    Here's one more non-precise trade-off.

                    Required vertical speed (VS) consists of two parts:
                    - VS to overcome gravity acceleration (VSG)
                    - VS to move vertically (VST)

                    VSG is almost unaffected by the environmental drag:
                    it slows the projectile similarly in both the up and the down ways.

                    But we need to adjust the VST:

                    VST += VST considering environmental drag.

                    P.S. If the source and the target are on the same vertical coordinates,
                    we don't need to adjust the VST to consider the environmental drag,
                    so the VST adjustment is 0.
                 */
                double vy = newVelocity.getY()
                        + targetLocation.getY() / ticksInFly * (dragFix - 1.0D);

                /*
                    Fix aiming of projectiles with gravity from long distances
                    with acute angles: check the approximation.
                 */
                int attempts = ARROW_DRAG_APPROXIMATION_ATTEMPTS;
                do {
                    attempts -= 1;

                    final double t_y = getAcceleratedAndDragged(
                            vy,
                            ticksInFly,
                            motion.getGravityAcceleration(),
                            motion.getDrag(),
                            motion.getDragAfter());

                    if(Math.abs(targetLocation.getY() - t_y) < DRAG_APPROXIMATION_EPSILON) {
                        break;
                    }

                    vy += (targetLocation.getY() - t_y) / ticksInFly * dragFix;

                } while (attempts > 0);

                newVelocity.setY(vy);
            } else {
                newVelocity.multiply(dragFix);
            }
        }
        // Add a little speed so that the projectile doesn't attend late for sure.
        // newVelocity.multiply(1.01D);

        projectile.setVelocity(newVelocity);

        if(customLogger.isDebugMode()) {
            customLogger.debug(String.format(
                    "Changed velocity of %s" +
                            " from %s to %s, ETA is %.2f ticks",
                    getDescription(projectileSource, projectile, targetPlayer, velocity),
                    format(initialProjectileVelocity), format(newVelocity), ticksInFly));
        }
    }

    /*
        Try #1

        Let's assume, we have a flying fireball.

        The Minecraft client is designed for fixed acceleration of fireballs,
        so we should keep its acceleration, but retarget better.

        We should assume the target moves.

        s = source (projectile)
        t = target (player)
        e = end point or "meeting" point

        The fireball in the end point
        xe = xs + axs * t^2 / 2
        ye = ys + ays * t^2 / 2
        ze = zs + azs * t^2 / 2

        Momentum
        axs^2 + ays^2 + azs^2 = r

        The target in the end point
        xe = xt + vxt * t
        ye = yt + vyt * t
        ze = zt + vzt * t

        In the long investigation above, we've already learned where this goes:
        a biquadratic equation that only a mother could love.

        Try #2

        a) Calculate the collision time without the environmental drag

        Since fireballs are accelerated, they will reach the target anyway
        if we accept the risk of quickly moving players and high environmental drag.

        Assume the target is moving on a flat line:

        acceleration * t^2 / 2 = distance + velocity * t =>
            acceleration / 2 * t^2 - velocity * t - distance = 0

        t = [-b +- (b^2 - 4ac) ^ 0.5] / 2a

        Since "a = acceleration" is always positive and "c = -distance" is always negative =>
            - 4ac > 0 =>
            (b^2 - 4ac) ^ 0.5 > b =>
            b = -velocity =>
            "[velocity <minus> - (velocity^2 - 4ac) ^ 0.5]" is always negative and doesn't work =>
            "[velocity <plus>  + (velocity^2 - 4ac) ^ 0.5]" is the only valid option

        t = [velocity + (velocity^2 + 4 * acceleration / 2 * distance) ^ 0.5] / (2 * acceleration / 2) =>
            t = [velocity + (velocity^2 + 2 * acceleration * distance) ^ 0.5] / (acceleration)

        b) Consider the environmental drag for a maximum velocity

        velocity = (velocity + acceleration) * (1 - drag) =>
            velocity = velocity + acceleration - velocity * drag - acceleration * drag =>
            0 = acceleration - velocity * drag - acceleration * drag =>
            velocity = (acceleration - acceleration * drag) / drag
            velocity = acceleration / drag - acceleration

        vm = max velocity considering the environmental drag
        vp = player velocity

        vm * t = distance + vp * t =>
            vm * t - vp * t = distance =>
            (vm - vp) * t = distance =>
            t = distance / (vm - vp)

        If maximum velocity considering the environmental drag is lower than player velocity,
        the equation has no solution

        c) Calculate a new fireball acceleration to more precise aiming.

        The calculation above assumes the player velocity vector is constant
        relative to the distance vector between the player and the fireball,
        which is true only if both vectors are directed in the same direction
         - so, actually, it is never true.

        However, no precise calculation was needed at that time.

        Finally, based on the time needed to collision,
        we'll calculate an exact projected player position
        and a new fireball acceleration needed to collide the player there in time.

        The final exact fireball acceleration will be different from the initial one
        to compensate for the trade-off made.

        Though the Minecraft client is designed for fixed acceleration of fireballs,
        we accept the risk of visually noticeable differences.

        https://minecraft.fandom.com/wiki/Entity#Motion_of_entities, section "Motion of entities"

        Note that when living entities and explosive projectiles are simulated,
        the drag is applied after the acceleration, rather than before.
     */
    private void aimFireball(final LivingEntity projectileSource, final Fireball fireball, final Player targetPlayer) throws FireballAdapterNotSupported {
        final Vector velocity = plugin.getPlayerTracker().getPlayerMovementVector(targetPlayer);
        final Vector acceleration = FireballAdapter.getAcceleration(fireball);
        final ProjectileMotion motion = ProjectileMotion.getProjectileMotion(fireball);

        final Location location = getAimPoint(targetPlayer);
        location.subtract(fireball.getLocation());

        double ticksToCollide;
        final boolean isLowDrag;
        {
            final double lowDragTicksToCollide;
            {
                lowDragTicksToCollide = velocity.length()
                        + Math.pow(Math.pow(velocity.length(), 2.0D) + 2.0D * acceleration.length() * location.length(), 0.5D)
                        / (acceleration.length());
            }

            final double highDragTicksToCollide;
            {
                final double maxFireballVelocity = acceleration.length() / motion.getDrag() - acceleration.length();
                if (maxFireballVelocity <= velocity.length()) {
                    customLogger.warning(String.format(
                            "Can't modify acceleration %s of %s" +
                                    " with player velocity %.2f: max fireball velocity is %.2f",
                            format(acceleration), getDescription(projectileSource, fireball, targetPlayer, velocity),
                            velocity.length(), maxFireballVelocity));
                    return;
                }

                // When a fireball accelerates, it moves slower initially.
                // Empirically, it loses (1 - drag) of total distance on that phase.
                highDragTicksToCollide = (location.length() + (1.0D - motion.getDrag()))
                        / (maxFireballVelocity - velocity.length());
            }

            if(lowDragTicksToCollide > highDragTicksToCollide) {
                isLowDrag = true;
                ticksToCollide = lowDragTicksToCollide;
            } else {
                isLowDrag = false;
                ticksToCollide = highDragTicksToCollide;
            }
        }

        Location targetLocation;

        int attempts = FIREBALL_DRAG_APPROXIMATION_ATTEMPTS;
        do {
            attempts -= 1;

            targetLocation = location.clone()
                    .add(velocity.clone().multiply(ticksToCollide));

            final double targetDistance = targetLocation.length();

            final double actualDistance = getAcceleratedAndDragged(
                    0.0D,
                    ticksToCollide,
                    acceleration.length(),
                    motion.getDrag(),
                    motion.getDragAfter());

            /*customLogger.info(String.format("Attempt to aim of %s: " +
                            "drag %.2f, is-low-drag %b, " +
                            "static distance %.2f, ticks to collide %.2f, target distance %.2f, " +
                            "actual distance %.2f, attempts left %d",
                    getDescription(projectileSource, fireball, targetPlayer),
                    motion.getDrag(), isLowDrag,
                    location.length(), ticksToCollide, targetDistance,
                    actualDistance, attempts));*/

            if(Math.abs(actualDistance - targetDistance) < DRAG_APPROXIMATION_EPSILON) {
                break;
            }
            if(isLowDrag) {
                ticksToCollide = ticksToCollide * Math.sqrt(targetDistance / actualDistance);
            } else {
                final double playerVelocity = (targetDistance - location.length()) / ticksToCollide;
                final double maxFireballVelocity = acceleration.length() / motion.getDrag() - acceleration.length();
                if (maxFireballVelocity <= playerVelocity) {
                    customLogger.warning(String.format(
                            "Can't modify acceleration %s of %s" +
                                    " with player velocity %.2f for %.2fs: max fireball velocity is %.2f",
                            format(acceleration), getDescription(projectileSource, fireball, targetPlayer, velocity),
                            playerVelocity, ticksToCollide, maxFireballVelocity));
                    return;
                } else {
                    ticksToCollide += (targetDistance - actualDistance) / (maxFireballVelocity - playerVelocity);
                }
            }
        } while (attempts > 0);

        final Vector newAcceleration = targetLocation.toVector();
        newAcceleration.multiply(acceleration.length() / newAcceleration.length());
        FireballAdapter.setAcceleration(fireball, newAcceleration);

        if(attempts <= 0) {
            customLogger.warning(String.format(
                    "Changed acceleration of %s" +
                            " from %s to %s, ETA is %.2f ticks, no attempts left",
                    getDescription(projectileSource, fireball, targetPlayer, velocity),
                    format(acceleration), format(newAcceleration), ticksToCollide));
        } else if(customLogger.isDebugMode()) {
            customLogger.debug(String.format(
                    "Changed acceleration of %s" +
                            " from %s to %s, ETA is %.2f ticks, %d attempts left",
                    getDescription(projectileSource, fireball, targetPlayer, velocity),
                    format(acceleration), format(newAcceleration), ticksToCollide, attempts));
        }
    }

    private double getAcceleratedAndDragged(double velocity,
                                            final double doubleTicks,
                                            final double acceleration,
                                            final double drag,
                                            final boolean dragAfter) {
        // Apply velocity 1st time before gravity and drag
        double position = velocity;
        int intTicks = (int)Math.floor(doubleTicks);
        for (int i = 0; i < intTicks; i++) {
            if(dragAfter) {
                velocity += acceleration;
                velocity *= (1 - drag);
            } else {
                velocity *= (1 - drag);
                velocity += acceleration;
            }
            position += velocity;
        }
        // Apply the last, potentially partial tick
        if(doubleTicks > intTicks) {
            if(dragAfter) {
                velocity += acceleration;
                velocity *= (1 - drag);
            } else {
                velocity *= (1 - drag);
                velocity += acceleration;
            }
            position += velocity * (doubleTicks - intTicks);
        }

        return position;
    }

    private Location getAimPoint(final Player targetPlayer) {
        return targetPlayer.getLocation()
                .add(targetPlayer.getEyeLocation())
                .multiply(0.5D);
    }

    private String getDescription(final LivingEntity projectileSource,
                                  final Projectile projectile,
                                  final Player targetPlayer,
                                  final Vector targetVelocity) {
        return String.format("%s launched by %s targeted at %s moving with %s",
                format(projectile), format(projectileSource), format(targetPlayer), format(targetVelocity));
    }
}