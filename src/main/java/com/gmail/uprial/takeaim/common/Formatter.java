package com.gmail.uprial.takeaim.common;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public final class Formatter {
    public static String format(Entity entity) {
        if (entity == null) {
            return "null";
        }
        Location location = entity.getLocation();
        return String.format("%s[w: %s, x: %.0f, y: %.0f, z: %.0f, hp: %.2f, id: %s]",
                entity.getType(),
                (location.getWorld() != null) ? location.getWorld().getName() : "empty",
                location.getX(), location.getY(), location.getZ(),
                (entity instanceof LivingEntity) ? ((LivingEntity) entity).getHealth() : -1,
                entity.getUniqueId());
    }

    public static String format(Vector vector) {
        if(vector == null) {
            return "null";
        }
        return String.format("{x: %.2f, y: %.2f, z: %.2f, len: %.2f}",
                vector.getX(), vector.getY(), vector.getZ(), vector.length());
    }
}
