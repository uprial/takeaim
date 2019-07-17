package com.gmail.uprial.takeaim.trackers;

import org.bukkit.scheduler.BukkitRunnable;

public class TrackerTask<T extends Runnable> extends BukkitRunnable {
    private final T tracker;

    TrackerTask(T tracker) {
        this.tracker = tracker;
    }

    @Override
    public void run() {
        tracker.run();
    }
}