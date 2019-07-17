package com.gmail.uprial.takeaim.tracker;

import com.gmail.uprial.takeaim.TakeAim;
import com.gmail.uprial.takeaim.common.CustomLogger;
import org.bukkit.entity.Projectile;

import java.util.*;

import static com.gmail.uprial.takeaim.common.Formatter.format;

public class ProjectileTracker {
    private static final int INTERVAL = 1;

    private final TakeAim plugin;
    private final CustomLogger customLogger;

    private final ProjectileTrackerTask task;

    private final Set<Projectile> projectiles = new HashSet<>();

    private boolean enabled = false;

    public ProjectileTracker(TakeAim plugin, CustomLogger customLogger) {
        this.plugin = plugin;
        this.customLogger = customLogger;

        task = new ProjectileTrackerTask(this);
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
            log(projectile, "launched");

            projectiles.add(projectile);
        }
    }

    public void onHit(Projectile projectile) {
        if(enabled) {
            log(projectile, "hit");

            projectiles.remove(projectile);
        }
    }

    void run() {
        if(enabled) {
            for (Projectile projectile : projectiles) {
                if (projectile.isDead() || !projectile.isValid() || projectile.isOnGround()) {
                    onHit(projectile);
                } else {
                    log(projectile, "is flying");

                }
            }
        }
    }

    private void setEnabled(boolean enabled) {
        if(this.enabled != enabled) {
            if (enabled) {
                if (task.isCancelled()) {
                    task.runTaskTimer(plugin, INTERVAL, INTERVAL);
                }
            } else {
                if (!task.isCancelled()) {
                    projectiles.clear();
                    task.cancel();
                }
            }

            this.enabled = enabled;
        }
    }

    private void log(Projectile projectile, String action) {
        customLogger.debug(String.format("Projectile %s %s with velocity %s",
                format(projectile), action, format(projectile.getVelocity())));
    }
}