package com.gmail.uprial.takeaim.ballistics;

import org.bukkit.entity.*;

import java.util.HashMap;
import java.util.Map;

public class ProjectileMotion {
    public static final double DEFAULT_PLAYER_ACCELERATION = -0.0784;

    private static final double epsilon = 1.0E-6D;

    final private double gravityAcceleration;
    final private double drag;
    final private boolean dragAfter;

    final private static Map<EntityType,ProjectileMotion> CACHE = new HashMap<>();

    private ProjectileMotion(final double gravityAcceleration,
                             final double drag,
                             final boolean dragAfter) {
        this.gravityAcceleration = gravityAcceleration;
        this.drag = drag;
        this.dragAfter = dragAfter;
    }

    double getGravityAcceleration() {
        return gravityAcceleration;
    }

    double getDrag() {
        return drag;
    }

    boolean hasGravityAcceleration() {
        return gravityAcceleration < - epsilon;
    }

    boolean hasDrag() {
        return drag > epsilon;
    }

    boolean getDragAfter() {
        return dragAfter;
    }

    static ProjectileMotion getProjectileMotion(final Projectile projectile) {
        final EntityType projectileType = projectile.getType();

        final ProjectileMotion motion;
        if(CACHE.containsKey(projectileType)) {
            motion = CACHE.get(projectileType);
        } else {
            motion = getProjectileMotionWithoutCache(projectile);
            CACHE.put(projectileType, motion);
        }

        return motion;
    }
    /*
        Gravity acceleration, m/s, depending on the projectile type.

        https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/Projectile.html (version 1.20.6)

        AbstractArrow           - Arrow
            Arrow
                TippedArrow
            SpectralArrow
            TippedArrow
            Trident (WARNING: also extends ThrowableProjectile)
        AbstractWindCharge      - ???
            BreezeWindCharge
            WindCharge
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


        https://minecraft.wiki/w/Entity#Motion, section "Motion of entities"
        * Explosive projectiles are not affected by gravity but instead get acceleration from getting damaged.
        * Dangerous wither skulls have drag force of 0.27.
     */
    private static ProjectileMotion getProjectileMotionWithoutCache(final Projectile projectile) {
        /*
            Compare a projectile class name for backward compatibility with 1.10.
            https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/AbstractArrow.html

            For 1.16 the condition below would be much simpler:
            if ((projectile instanceof AbstractArrow)){
         */
        if(projectile.getClass().getName().endsWith("Arrow")
            || projectile.getClass().getName().endsWith("Trident")) {
            return new ProjectileMotion(-0.05, 0.01, false);
        } else if (projectile instanceof WitherSkull) { // sub-instance of Fireball
            return new ProjectileMotion(0.0, 0.27, true);
        } else if (projectile instanceof Fireball) {
            return new ProjectileMotion(0.0, 0.05, true);
        } else if ((projectile instanceof Egg) || (projectile instanceof Snowball)
                || (projectile instanceof EnderPearl) || (projectile instanceof ThrownPotion)) {
            return new ProjectileMotion(-0.03, 0.01, false);
        } else {
            return null;
        }
    }
}