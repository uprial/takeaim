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

    private static final String MK_TARGET_PLAYER_UUID = "target-player-uuid";

    public TakeAimAttackEventListener(TakeAim plugin, CustomLogger customLogger) {
        this.plugin = plugin;
        this.customLogger = customLogger;
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityTargetEvent(EntityTargetEvent event) {
        if ((plugin.getTakeAimConfig().isEnabled()) && (!event.isCancelled())) {
            Entity source = event.getEntity();
            // Performance improvement: ProjectileSource instead of LivingEntity
            if (source instanceof ProjectileSource) {
                LivingEntity projectileSource = (LivingEntity) source;
                Entity target = event.getTarget();
                if (target instanceof Player) {
                    Player targetPlayer = (Player) target;
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
            Projectile projectile = event.getEntity();
            if (hasGravityAcceleration(projectile)) {
                ProjectileSource shooter = projectile.getShooter();
                if (shooter instanceof LivingEntity) {
                    LivingEntity projectileSource = (LivingEntity) shooter;
                    UUID targetPlayerUUID = getMetadata(projectileSource, MK_TARGET_PLAYER_UUID);
                    if (targetPlayerUUID != null) {
                        Player targetPlayer = plugin.getOnlinePlayerByUUID(targetPlayerUUID);
                        if (targetPlayer != null) {
                            fixProjectileTrajectory(projectileSource, projectile, targetPlayer);
                        }
                    }
                }
            }
        }
    }

    private void fixProjectileTrajectory(LivingEntity projectileSource, Projectile projectile, Player targetPlayer) {
        Location targetLocation = targetPlayer.getEyeLocation();
        Vector projectileVelocity = projectile.getVelocity();
        Location projectileLocation = projectile.getLocation();

        double ticksInFly = targetLocation.distance(projectile.getLocation()) / projectileVelocity.length();

        double vx = (targetLocation.getX() - projectileLocation.getX()) / ticksInFly;
        double vz = (targetLocation.getZ() - projectileLocation.getZ()) / ticksInFly;

        /*
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
         */

        double y1 = 0.0;
        double y2 = targetLocation.getY() - projectileLocation.getY();
        double t = ticksInFly / SERVER_TICKS_IN_SECOND;
        double vy = ((y2 - ((getGravityAcceleration(projectile) * (t * t)) / 2.0) - y1) / t) / SERVER_TICKS_IN_SECOND;

        // Consider the target player is running somewhere ...
        Vector targetVelocity = getPlayerMovementVector(targetPlayer);
        vx += targetVelocity.getX();
        vy += targetVelocity.getY();
        vz += targetVelocity.getZ();

        Vector newVelocity = new Vector(vx, vy, vz);
        projectile.setVelocity(newVelocity);

        if(customLogger.isDebugMode()) {
            customLogger.debug(String.format("Modify projective's velocity of %s on %s from %s to %s",
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
