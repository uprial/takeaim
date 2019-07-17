package com.gmail.uprial.takeaim.trackers;

import com.gmail.uprial.takeaim.TakeAim;
import com.gmail.uprial.takeaim.common.CustomLogger;
import org.bukkit.entity.Projectile;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.gmail.uprial.takeaim.common.Formatter.format;

public class ProjectileTracker implements Runnable {
    private static final int INTERVAL = 1;

    private final TakeAim plugin;
    private final CustomLogger customLogger;

    private TrackerTask<ProjectileTracker> task;

    private final Map<Projectile,Integer> projectiles = new HashMap<>();
    private final AtomicInteger projectileIdIncrement = new AtomicInteger();

    private boolean enabled = false;

    public ProjectileTracker(TakeAim plugin, CustomLogger customLogger) {
        this.plugin = plugin;
        this.customLogger = customLogger;

        onConfigChange();
    }

    public void stop() {
        setEnabled(false);
    }

    public void onConfigChange() {
        setEnabled(plugin.getTakeAimConfig().isEnabled() && customLogger.isDebugMode());
    }

    public void onLaunch(Projectile projectile) {
        if(enabled) {
            int projectileId = projectileIdIncrement.incrementAndGet();
            log(projectileId, projectile, "has been launched");

            projectiles.put(projectile, projectileId);
        }
    }

    public void onHit(Projectile projectile) {
        if(enabled) {
            Integer projectileId = projectiles.get(projectile);
            if(projectileId == null) {
                log(0, projectile, "hit the target");

            } else {
                log(projectileId, projectile, "hit the target");

                projectiles.remove(projectile);
            }
        }
    }

    @Override
    public void run() {
        if(enabled) {
            for (Map.Entry<Projectile, Integer> entry : projectiles.entrySet()) {
                Projectile projectile = entry.getKey();

                if (projectile.isDead() || !projectile.isValid() || projectile.isOnGround()) {
                    onHit(projectile);
                } else {
                    log(entry.getValue(), projectile, "is flying");

                }
            }
        }
    }

    private void setEnabled(boolean enabled) {
        if(this.enabled != enabled) {
            if (enabled) {
                task = new TrackerTask<>(this);
                task.runTaskTimer(plugin, INTERVAL, INTERVAL);
            } else {
                task.cancel();
                task = null;

                projectiles.clear();
                projectileIdIncrement.set(0);
            }

            this.enabled = enabled;
        }
    }

    private void log(int projectileId, Projectile projectile, String action) {
        customLogger.debug(String.format("#%d %s %s with %s",
                projectileId, format(projectile), action, format(projectile.getVelocity())));
    }
}