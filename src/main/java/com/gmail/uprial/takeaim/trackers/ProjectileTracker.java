package com.gmail.uprial.takeaim.trackers;

import com.gmail.uprial.takeaim.TakeAim;
import com.gmail.uprial.takeaim.common.CustomLogger;
import org.bukkit.entity.Projectile;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.gmail.uprial.takeaim.common.Formatter.format;

public class ProjectileTracker extends AbstractTracker {
    private static final int INTERVAL = 1;

    private final TakeAim plugin;
    private final CustomLogger customLogger;

    private final Map<Projectile,Integer> projectiles = new HashMap<>();
    private final AtomicInteger projectileIdIncrement = new AtomicInteger();

    public ProjectileTracker(TakeAim plugin, CustomLogger customLogger) {
        super(plugin, INTERVAL);

        this.plugin = plugin;
        this.customLogger = customLogger;
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
            List<Projectile> onHit = new ArrayList<>();
            for (Map.Entry<Projectile, Integer> entry : projectiles.entrySet()) {
                Projectile projectile = entry.getKey();

                if (projectile.isDead() || !projectile.isValid() || projectile.isOnGround()) {
                    onHit.add(projectile);
                } else {
                    log(entry.getValue(), projectile, "is flying");

                }
            }
            for(Projectile projectile : onHit) {
                onHit(projectile);
            }
        }
    }

    @Override
    protected void clear() {
        projectiles.clear();
        projectileIdIncrement.set(0);
    }

    @Override
    protected boolean isEnabled() {
        return plugin.getTakeAimConfig().isEnabled() && customLogger.isDebugMode();
    }

    private void log(int projectileId, Projectile projectile, String action) {
        customLogger.debug(String.format("#%d %s %s with %s",
                projectileId, format(projectile), action, format(projectile.getVelocity())));
    }
}