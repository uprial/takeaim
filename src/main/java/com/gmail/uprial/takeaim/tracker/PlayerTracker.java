package com.gmail.uprial.takeaim.tracker;

import com.gmail.uprial.takeaim.TakeAim;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.gmail.uprial.takeaim.common.Utils.SERVER_TICKS_IN_SECOND;

public class PlayerTracker {
    private static final int INTERVAL = SERVER_TICKS_IN_SECOND / 2;

    private final TakeAim plugin;

    private final PlayerTrackerTask task;

    private final Map<UUID, Map<Boolean, Location>> players = new HashMap<>();
    private boolean side = true;

    public PlayerTracker(TakeAim plugin) {
        this.plugin = plugin;

        task = new PlayerTrackerTask(this);
        task.runTaskTimer(plugin, INTERVAL, INTERVAL);
    }

    public void stop() {
        task.cancel();
        players.clear();
    }

    public Vector getPlayerMovementVector(Player player) {
        UUID uuid = player.getUniqueId();
        Map<Boolean, Location> bucket = players.get(uuid);
        if(bucket != null) {
            Location CurrentLocation = bucket.get(side);
            Location OldLocation = bucket.get(!side);
            if((CurrentLocation != null) || (OldLocation != null)) {
                return new Vector(
                        (CurrentLocation.getX() - OldLocation.getX()) / INTERVAL,
                        (CurrentLocation.getY() - OldLocation.getY()) / INTERVAL,
                        (CurrentLocation.getZ() - OldLocation.getZ()) / INTERVAL
                );
            }
        }
        return new Vector(0.0, 0.0, 0.0);
    }

    public Player getOnlinePlayerByUUID(UUID uuid) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getUniqueId().equals(uuid)) {
                return player;
            }
        }

        return null;
    }

    void run() {
        if(plugin.getTakeAimConfig().isEnabled()){
            side = !side;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                Map<Boolean, Location> bucket;
                if (players.containsKey(uuid)) {
                    bucket = players.get(uuid);
                } else {
                    bucket = new HashMap<>();
                    players.put(uuid, bucket);
                }
                bucket.put(side, player.getLocation());

            }
        }
    }
}