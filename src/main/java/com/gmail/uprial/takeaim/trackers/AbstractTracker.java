package com.gmail.uprial.takeaim.trackers;

import org.bukkit.plugin.java.JavaPlugin;

abstract class AbstractTracker implements Runnable {
    private final JavaPlugin plugin;
    private final int interval;

    private TrackerTask<AbstractTracker> task;

    boolean enabled = false;

    AbstractTracker(final JavaPlugin plugin, final int interval) {
        this.plugin = plugin;
        this.interval = interval;
    }

    public void stop() {
        setEnabled(false);
    }

    public void onConfigChange() {
        setEnabled(isEnabled());
    }

    abstract boolean isEnabled();

    abstract void clear();

    private void setEnabled(final boolean enabled) {
        if(this.enabled != enabled) {
            if (enabled) {
                task = new TrackerTask<>(this);
                task.runTaskTimer(plugin, interval, interval);
            } else {
                task.cancel();
                task = null;
                clear();
            }

            this.enabled = enabled;
        }
    }
}
