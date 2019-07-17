package com.gmail.uprial.takeaim.listeners;

import com.gmail.uprial.takeaim.TakeAim;
import com.gmail.uprial.takeaim.ballistics.ProjectileHoming;
import com.gmail.uprial.takeaim.common.CustomLogger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

import static com.gmail.uprial.takeaim.common.MetadataHelper.getMetadata;
import static com.gmail.uprial.takeaim.common.MetadataHelper.setMetadata;

public class TakeAimAttackEventListener implements Listener {
    private final TakeAim plugin;
    private final ProjectileHoming homing;

    private static final String MK_TARGET_PLAYER_UUID = "target-player-uuid";

    public TakeAimAttackEventListener(TakeAim plugin, CustomLogger customLogger) {
        this.plugin = plugin;
        homing = new ProjectileHoming(plugin, customLogger);
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

            if (homing.hasProjectileMotion(projectile)) {
                final ProjectileSource shooter = projectile.getShooter();
                if (shooter instanceof LivingEntity) {
                    final LivingEntity projectileSource = (LivingEntity) shooter;
                    final UUID targetPlayerUUID = getMetadata(projectileSource, MK_TARGET_PLAYER_UUID);
                    if (targetPlayerUUID != null) {
                        final Player targetPlayer = plugin.getPlayerTracker().getOnlinePlayerByUUID(targetPlayerUUID);
                        if (targetPlayer != null) {
                            homing.aimProjectile(projectileSource, projectile, targetPlayer);
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
}
