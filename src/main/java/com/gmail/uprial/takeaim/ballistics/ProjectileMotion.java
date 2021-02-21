package com.gmail.uprial.takeaim.ballistics;

import org.bukkit.entity.*;

import java.util.HashMap;
import java.util.Map;

public class ProjectileMotion {
    public static final double PLAYER_ACCELERATION = -0.08;

    private static final double epsilon = 1.0E-6D;

    final private double acceleration;
    final private double drag;

    final private static Map<EntityType,ProjectileMotion> CACHE = new HashMap<>();

    private ProjectileMotion(final double acceleration, final double drag) {
        this.acceleration = acceleration;
        this.drag = drag;
    }

    double getAcceleration() {
        return acceleration;
    }

    double getDrag() {
        return drag;
    }

    boolean hasAcceleration() {
        return acceleration < - epsilon;
    }

    boolean hasDrag() {
        return drag > epsilon;
    }

    static ProjectileMotion getProjectileMotion(final Projectile projectile) {
        final EntityType projectileType = projectile.getType();

        ProjectileMotion motion = CACHE.get(projectileType);
        if(motion == null) {
            motion = getProjectileMotionWithoutCache(projectile);
            CACHE.put(projectileType, motion);
        }

        return motion;
    }
    /*
        Gravity acceleration, m/s, depending on the projectile type.

        https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/Projectile.html (version 1.16.5)

        AbstractArrow           - Arrow
            Arrow
                TippedArrow
            SpectralArrow
            TippedArrow
            Trident (WARNING: also extends ThrowableProjectile)
        ThrowableProjectile     - Thrown
            Egg
            EnderPearl
            Snowball
            ThrownExpBottle (WARNING: excluded from getProjectileMotionWithoutCache() because nobody throws it into a player)
        Fireball                - Fireball
            DragonFireball
            SizedFireball
                LargeFireball
                SmallFireball
            WitherSkull
        Firework                - ???
        FishHook                - ???
        LlamaSpit               - ???
        ShulkerBullet           - ???
        ThrownPotion            - Thrown
            LingeringPotion
            SplashPotion


                    Acceleration    Drag
                    m/s^2           1/tick
        Thrown      -12.0           0.01
        Arrow       -20.0           0.01
        Fireball    0.0             0.0


        https://minecraft.gamepedia.com/Entity
     */
    private static ProjectileMotion getProjectileMotionWithoutCache(final Projectile projectile) {
        if (projectile instanceof AbstractArrow) {
            return new ProjectileMotion(-20.0, 0.01);
        } else if ((projectile instanceof Egg) || (projectile instanceof EnderPearl)
                || (projectile instanceof Snowball) || (projectile instanceof ThrownPotion)) {
            return new ProjectileMotion(-12.0, 0.01);
        /*} else if (projectile instanceof Fireball) {
            return new ProjectileMotion(0.0, 0.0);*/
        } else {
            return null;
        }
    }
}