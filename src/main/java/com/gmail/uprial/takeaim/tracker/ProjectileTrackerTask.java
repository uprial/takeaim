package com.gmail.uprial.takeaim.tracker;

import org.bukkit.scheduler.BukkitRunnable;

public class ProjectileTrackerTask extends BukkitRunnable {
    private final ProjectileTracker tracker;

    ProjectileTrackerTask(ProjectileTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public void run() {
        tracker.run();
    }
}