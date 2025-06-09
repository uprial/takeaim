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
        final Location location = entity.getLocation();
        return String.format("%s[w: %s, x: %.2f, y: %.2f, z: %.2f, hp: %.2f, id: %s]",
                entity.getType(),
                (location.getWorld() != null) ? location.getWorld().getName() : "empty",
                location.getX(), location.getY(), location.getZ(),
                (entity instanceof LivingEntity) ? ((LivingEntity) entity).getHealth() : -1.0D,
                entity.getUniqueId().toString().substring(0, 8));
    }

    public static String format(Vector vector) {
        if(vector == null) {
            return "null";
        }
        return String.format("{x: %.2f, y: %.2f, z: %.2f, len: %.2f}",
                vector.getX(), vector.getY(), vector.getZ(), vector.length());
    }
}
