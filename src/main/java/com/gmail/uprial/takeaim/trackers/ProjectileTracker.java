package com.gmail.uprial.takeaim.trackers;

import com.gmail.uprial.takeaim.TakeAim;
import com.gmail.uprial.takeaim.common.CustomLogger;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.gmail.uprial.takeaim.common.Formatter.format;

public class ProjectileTracker extends AbstractTracker {
    private class ProjectileInfo {
        private final long id;
        private final Vector startPosition;

        private final AtomicLong ticksInFly = new AtomicLong();

        ProjectileInfo(final long id, final Vector startPosition) {
            this.id = id;
            this.startPosition = startPosition;
        }
    }

    private static final int INTERVAL = 1;

    private final TakeAim plugin;
    private final CustomLogger customLogger;

    private final Map<Projectile,ProjectileInfo> projectiles = new HashMap<>();
    private final AtomicLong projectileIdIncrement = new AtomicLong();

    public ProjectileTracker(final TakeAim plugin, final CustomLogger customLogger) {
        super(plugin, INTERVAL);

        this.plugin = plugin;
        this.customLogger = customLogger;

        onConfigChange();
    }

    public void onLaunch(final Projectile projectile) {
        if(enabled) {
            final long projectileId = projectileIdIncrement.incrementAndGet();
            final ProjectileInfo info = new ProjectileInfo(
                    projectileId,
                    projectile.getLocation().toVector());
            log(info, projectile, "has been launched");

            projectiles.put(projectile, info);
        }
    }

    public void onHit(final Projectile projectile) {
        if(enabled) {
            final ProjectileInfo info = projectiles.get(projectile);
            if(info == null) {
                log(null, projectile, "hit the target");

            } else {
                log(info, projectile, "hit the target");

                final Vector avgVelocity = projectile.getLocation().toVector()
                        .subtract(info.startPosition)
                        .multiply(1.0D / Math.max(1.0D, info.ticksInFly.get()));

                customLogger.debug(String.format("#%d %s average velocity: %s",
                        info.id, format(projectile), format(avgVelocity)));

                projectiles.remove(projectile);
            }
        }
    }

    @Override
    public void run() {
        if(enabled) {
            final List<Projectile> onHit = new ArrayList<>();
            for (Map.Entry<Projectile, ProjectileInfo> entry : projectiles.entrySet()) {
                final Projectile projectile = entry.getKey();

                if (projectile.isDead() || !projectile.isValid() || projectile.isOnGround()) {
                    onHit.add(projectile);
                } else {
                    final ProjectileInfo info = entry.getValue();
                    if((info.ticksInFly.get() > 0)
                        || (!projectile.getLocation().toVector().equals(info.startPosition))) {
                        info.ticksInFly.incrementAndGet();
                    }
                    log(info, projectile, "is flying");

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
        projectileIdIncrement.set(0L);
    }

    @Override
    protected boolean isEnabled() {
        return plugin.getTakeAimConfig().isEnabled() && customLogger.isDebugMode();
    }

    private void log(final ProjectileInfo info, final Projectile projectile, final String action) {
        if(info == null) {
            customLogger.debug(String.format("#? %s %s with %s, tick #?",
                    format(projectile), action, format(projectile.getVelocity())));
        } else {
            customLogger.debug(String.format("#%d %s %s with %s, tick #%d",
                    info.id, format(projectile), action, format(projectile.getVelocity()), info.ticksInFly.get()));
        }
    }
}