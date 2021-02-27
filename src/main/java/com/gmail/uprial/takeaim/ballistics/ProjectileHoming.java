package com.gmail.uprial.takeaim.ballistics;

import com.gmail.uprial.takeaim.TakeAim;
import com.gmail.uprial.takeaim.common.CustomLogger;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

import static com.gmail.uprial.takeaim.common.Formatter.format;
import static com.gmail.uprial.takeaim.common.Utils.SERVER_TICKS_IN_SECOND;

public class ProjectileHoming {
    private final TakeAim plugin;
    private final CustomLogger customLogger;

    private static final double MAX_ARROW_SPEED_PER_TICK = 3.0D;

    public ProjectileHoming(final TakeAim plugin, final CustomLogger customLogger) {
        this.plugin = plugin;
        this.customLogger = customLogger;
    }

    public boolean hasProjectileMotion(final Projectile projectile) {
        return ProjectileMotion.getProjectileMotion(projectile) != null;
    }

    /*
        Try #1

        Let's assume, we have a flying projectile.
        We should keep its momentum, but retarget better.
        We should assume the target moves.

        s = source
        t = target
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
        Now we should consider drag somehow which increases the degree.
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

        Sn = b1 * (1 - q^n) / (1 - q)
        b1 = Sn * (1 - q) / (1 - q^n)

        Now we have to check that our target isn't too far away.
        Assuming an experimental maximum momentum and an angle of 45 grades, let's calculate a maximum distance.

        m = MAX_ARROW_SPEED_PER_TICK
        t = cos(a) * m / g = sqrt(2) * m / g
        d = sin(a) * m * t = sqrt(2) * sqrt(2) * m * m / g = m^2 / g

     */
    public void aimProjectile(final LivingEntity projectileSource, final Projectile projectile, final Player targetPlayer) {
        final Location targetLocation = targetPlayer.getEyeLocation();
        // Normalize considering the projectile initial location.
        targetLocation.subtract(projectile.getLocation());

        final Vector initialProjectileVelocity = projectile.getVelocity();

        // How long the projectile will be enforced to fly to the target player.
        final double ticksInFly = Math.ceil(targetLocation.length() / initialProjectileVelocity.length());

        // Consider the target player is running somewhere.
        {
            final Vector targetVelocity = plugin.getPlayerTracker().getPlayerMovementVector(targetPlayer);
            targetVelocity.multiply(ticksInFly);
            targetLocation.add(targetVelocity);
        }

        final ProjectileMotion motion = ProjectileMotion.getProjectileMotion(projectile);

        if(motion.hasAcceleration()) {
            final double distance = targetLocation.length();
            final double maxDistance = -Math.pow(MAX_ARROW_SPEED_PER_TICK * SERVER_TICKS_IN_SECOND, 2.0D) / motion.getAcceleration();

            if (distance > maxDistance) {
                customLogger.warning(String.format("Can't modify projectile velocity of %s targeted at %s at a distance of %.2f. Max distance is %.2f",
                        format(projectileSource), format(targetPlayer), distance, maxDistance));
                return;
            }
        }

        final Vector newVelocity;
        {
            final double vx = targetLocation.getX() / ticksInFly;
            final double vz = targetLocation.getZ() / ticksInFly;

            final double vy;

            if (motion.hasAcceleration()) {
                final double y1 = 0.0D;
                final double y2 = targetLocation.getY();
                final double t = ticksInFly / SERVER_TICKS_IN_SECOND;

                vy = ((y2 - (motion.getAcceleration() * t * t / 2.0D) - y1) / t) / SERVER_TICKS_IN_SECOND;
            } else {
                vy = targetLocation.getY() / ticksInFly;
            }

            newVelocity = new Vector(vx, vy, vz);
        }

        // Consider a drag.
        if(motion.hasDrag()) {
            final double q = (1.0D - motion.getDrag());
            final double normalizedDistanceWithDrag = (1.0D - Math.pow(q, ticksInFly)) / (1.0D - q);
            newVelocity.multiply(ticksInFly / normalizedDistanceWithDrag);
        }
        // Add a little speed so that the projectile doesn't attend late for sure.
        newVelocity.multiply(1.01D);

        projectile.setVelocity(newVelocity);

        if(customLogger.isDebugMode()) {
            customLogger.debug(String.format("Changed velocity of %s launched by %s targeted at %s from %s to %s, ETA is %.0f ticks",
                    projectile.getType(), format(projectileSource), format(targetPlayer), format(initialProjectileVelocity), format(newVelocity), ticksInFly));
        }
    }
}
