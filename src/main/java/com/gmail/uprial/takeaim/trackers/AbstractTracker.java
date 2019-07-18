package com.gmail.uprial.takeaim.trackers;

import com.gmail.uprial.takeaim.TakeAim;

abstract class AbstractTracker implements Runnable {
    private final TakeAim plugin;
    private final int interval;

    private TrackerTask<AbstractTracker> task;

    boolean enabled = false;

    AbstractTracker(TakeAim plugin, int interval) {
        this.plugin = plugin;
        this.interval = interval;

        onConfigChange();
    }

    public void stop() {
        setEnabled(false);
    }

    public void onConfigChange() {
        setEnabled(isEnabled());
    }

    abstract boolean isEnabled();

    abstract void clear();

    private void setEnabled(boolean enabled) {
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
