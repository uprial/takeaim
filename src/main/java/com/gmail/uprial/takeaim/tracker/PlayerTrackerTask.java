package com.gmail.uprial.takeaim.tracker;

import org.bukkit.scheduler.BukkitRunnable;

public class PlayerTrackerTask extends BukkitRunnable {
    private final PlayerTracker tracker;

    PlayerTrackerTask(PlayerTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public void run() {
        tracker.run();
    }
}
