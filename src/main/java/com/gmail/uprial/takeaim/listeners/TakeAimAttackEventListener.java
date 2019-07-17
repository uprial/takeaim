package com.gmail.uprial.takeaim.listeners;

import com.gmail.uprial.takeaim.TakeAim;
import com.gmail.uprial.takeaim.common.CustomLogger;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.UUID;

import static com.gmail.uprial.takeaim.TakeAimPlayerTracker.getPlayerMovementVector;
import static com.gmail.uprial.takeaim.common.Formatter.format;
import static com.gmail.uprial.takeaim.common.MetadataHelper.getMetadata;
import static com.gmail.uprial.takeaim.common.MetadataHelper.setMetadata;
import static com.gmail.uprial.takeaim.common.Utils.SERVER_TICKS_IN_SECOND;

public class TakeAimAttackEventListener implements Listener {
    final TakeAim plugin;
    final CustomLogger customLogger;

    private static final double MAX_ARROW_SPEED_PER_TICK = 3.0;

    private static final String MK_TARGET_PLAYER_UUID = "target-player-uuid";

    public TakeAimAttackEventListener(TakeAim plugin, CustomLogger customLogger) {
        this.plugin = plugin;
        this.customLogger = customLogger;
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityTargetEvent(EntityTargetEvent event) {
        if ((plugin.getTakeAimConfig().isEnabled()) && (!event.isCancelled())) {
            final Entity source = event.getEntity();
            // Performance improvement: ProjectileSource instead of LivingEntity
            if (source instanceof ProjectileSource) {
                final LivingEntity projectileSource = (LivingEntity) source;
                final Entity target = event.getTarget();
                if (target instanceof Player) {
                    final Player targetPlayer = (Player) target;
                    setMetadata(plugin, projectileSource, MK_TARGET_PLAYER_UUID, targetPlayer.getUniqueId());
                } else {
                    // Clear the target
                    setMetadata(plugin, projectileSource, MK_TARGET_PLAYER_UUID, null);
                }
            }
        }
    }

    @SuppressWarnings({"unused", "MethodMayBeStatic"})
    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileLaunchEvent(ProjectileLaunchEvent event) {
        if ((plugin.getTakeAimConfig().isEnabled()) && (!event.isCancelled())) {
            final Projectile projectile = event.getEntity();

            plugin.getProjectileTracker().onLaunch(projectile);

            if (hasGravityAcceleration(projectile)) {
                final ProjectileSource shooter = projectile.getShooter();
                if (shooter instanceof LivingEntity) {
                    final LivingEntity projectileSource = (LivingEntity) shooter;
                    final UUID targetPlayerUUID = getMetadata(projectileSource, MK_TARGET_PLAYER_UUID);
                    if (targetPlayerUUID != null) {
                        final Player targetPlayer = plugin.getPlayerTracker().getOnlinePlayerByUUID(targetPlayerUUID);
                        if (targetPlayer != null) {
                            fixProjectileTrajectory(projectileSource, projectile, targetPlayer);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings({"unused", "MethodMayBeStatic"})
    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileHitEvent(ProjectileHitEvent event) {
        if ((plugin.getTakeAimConfig().isEnabled())) {
            plugin.getProjectileTracker().onHit(event.getEntity());
        }
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
        vxs = (xt + vxt * t - xs) /  t = vxt + (xt - xs)/t
        vys = yt + vyt * t - (g * t^2 / 2 + ys) / t
        vzs = (zt + vzt * t - zs)) / t= vzt + (zt - zs)/t
        vxs^2 + vys^2 + vzs^2 = r

        So, we have a biquadratic equation that only a mother could love. Let's try to simplify the case.

        Try #2

        Assume, we don't need to keep the momentum because we control a non-player entity anyway.
        We have to make sure two things happen:
        - a projectile does not fly too fast,
        - the projectile meets the target exactly at the time.

        So, we need to calculate only Y axis:

        y2 = y1 + vy * t + g * t^2 / 2 =>
            vy = (y2 - g * t^2 / 2 - y1) / t

        g = GRAVITY_ACCELERATION
        t = ticksInFly (ticks) / t2s (ticks/s)

        Example:
          y1 = 0
          y2 = 0
          ticksInFly = 10.0

          t = 10.0 / 20.0 = 0.5,
          vy = (0 - (- 20.0 * 0.5^2 / 2) - 0) / 0.5 =:= 20.0 / 4 =:= 5 (m/s)


        Now we have to check that our target isn't too far away.
        Assuming an experimental maximum momentum and an angle of 45 grades, let's calculate a maximum distance.

        m = MAX_ARROW_SPEED_PER_TICK
        t = cos(a) * m / g = sqrt(2) * m / g
        d = sin(a) * m * t = sqrt(2) * sqrt(2) * m * m / g = m^2 / g

     */
    private void fixProjectileTrajectory(LivingEntity projectileSource, Projectile projectile, Player targetPlayer) {
        Location targetLocation = targetPlayer.getEyeLocation();
        Location projectileLocation = projectile.getLocation();

        double g = getGravityAcceleration(projectile);
        double distance = targetLocation.distance(projectile.getLocation());

        double maxDistance = - Math.pow(MAX_ARROW_SPEED_PER_TICK * SERVER_TICKS_IN_SECOND, 2.0) / g;

        if(distance > maxDistance) {
            customLogger.warning(String.format("Can't modify projectile velocity of %s targeted at %s at a distance of %.2f. Max distance is %.2f",
                    format(projectileSource), format(targetPlayer), distance, maxDistance));
            return;
        }

        Vector projectileVelocity = projectile.getVelocity();
        double ticksInFly = distance / projectileVelocity.length();

        double vx = (targetLocation.getX() - projectileLocation.getX()) / ticksInFly;
        double vz = (targetLocation.getZ() - projectileLocation.getZ()) / ticksInFly;

        double y1 = 0.0;
        double y2 = targetLocation.getY() - projectileLocation.getY();
        double t = ticksInFly / SERVER_TICKS_IN_SECOND;
        double vy = ((y2 - (g * t * t / 2.0) - y1) / t) / SERVER_TICKS_IN_SECOND;

        // Consider the target player is running somewhere ...
        Vector targetVelocity = plugin.getPlayerTracker().getPlayerMovementVector(targetPlayer);
        vx += targetVelocity.getX();
        vy += targetVelocity.getY();
        vz += targetVelocity.getZ();

        Vector newVelocity = new Vector(vx, vy, vz);
        projectile.setVelocity(newVelocity);

        if(customLogger.isDebugMode()) {
            customLogger.debug(String.format("Modify projectile velocity of %s targeted at %s from %s to %s",
                    format(projectileSource), format(targetPlayer), format(projectileVelocity), format(newVelocity)));
        }
    }

    /*
        Gravity acceleration, m/s, depending on the projectile type.

        https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/Projectile.html

        AbstractArrow
            Arrow
                TippedArrow
            SpectralArrow
            TippedArrow
            Trident
        Egg
        EnderPearl
        Fireball
            DragonFireball
            LargeFireball
            SmallFireball
            WitherSkull
        FishHook
        LlamaSpit -- unknown
        ShulkerBullet
        Snowball
        ThrownExpBottle
        ThrownPotion
            LingeringPotion
            SplashPotion


        https://minecraft.gamepedia.com/Entity
     */
    private double getGravityAcceleration(Projectile projectile) {
        if (projectile instanceof AbstractArrow) {
            return -20.0;
        } else if ((projectile instanceof Egg) || (projectile instanceof EnderPearl)
                || (projectile instanceof Snowball) || (projectile instanceof ThrownPotion)) {
            return -12.0;
        } else {
            return 0;
        }
    }

    private boolean hasGravityAcceleration(Projectile projectile) {
        return getGravityAcceleration(projectile) < 0.0;
    }
}
