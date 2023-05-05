package com.gmail.uprial.takeaim.ballistics;

import org.bukkit.entity.*;

import java.util.HashMap;
import java.util.Map;

public class ProjectileMotion {
    public static final double DEFAULT_PLAYER_ACCELERATION = -0.0784;

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


        https://minecraft.gamepedia.com/Entity, section "Motion of entities"
        * Explosive projectiles are not affected by gravity but instead get acceleration from getting damaged.
        * Dangerous wither skulls have drag force of 0.27.

                    Acceleration    Drag
                    m/s^2           1/tick
        Thrown      -12.0           0.01
        Arrow       -20.0           0.01
        Fireball    0.0             0.05
        WitherSkull 0.0             0.27

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
            return new ProjectileMotion(-20.0, 0.01);
        } else if ((projectile instanceof Egg) || (projectile instanceof Snowball)
                || (projectile instanceof EnderPearl) || (projectile instanceof ThrownPotion)) {
            return new ProjectileMotion(-12.0, 0.01);
        /*
            Fireballs fly straight and do not take setVelocity(...) well.
            https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/Fireball.html

            Let's track how Fireballs fly.

            [12:49:10] [Server thread/INFO]: [TakeAim] [DEBUG]
            #1 FIREBALL[world: world, x: 4, y: 67, z: -182] average velocity was [x: 0.54, y: -0.16, z: -0.35, len: 0.66] in 17 ticks
            #2 FIREBALL[world: world, x: 6, y: 68, z: -174] average velocity was [x: 0.28, y: -0.57, z: -0.09, len: 0.64] in 16 ticks
            #3 FIREBALL[world: world, x: -4, y: 68, z: -174] average velocity was [x: -0.32, y: -0.78, z: -0.25, len: 0.88] in 26 ticks
            #4 FIREBALL[world: world, x: 2, y: 68, z: -178] average velocity was [x: 0.09, y: -0.58, z: -0.71, len: 0.92] in 28 ticks
            #5 FIREBALL[world: world, x: 12, y: 68, z: -176] average velocity was [x: 0.45, y: -0.61, z: -0.71, len: 1.04] in 35 ticks
            #6 FIREBALL[world: world, x: 15, y: 68, z: -169] average velocity was [x: 0.53, y: -0.84, z: -0.46, len: 1.10] in 39 ticks
            #7 FIREBALL[world: world, x: 20, y: 69, z: -160] average velocity was [x: 0.71, y: -0.89, z: -0.03, len: 1.14] in 42 ticks
            #8 FIREBALL[world: world, x: 20, y: 69, z: -165] average velocity was [x: 0.57, y: -1.08, z: -0.11, len: 1.22] in 49 ticks


            Let's assume a Fireball has an linear acceleration.

            ve1 = v0 + vs * t1
            ve2 = v0 + vs * t2

            v0 = ve1 - vs * t1
            v0 = ve2 - vs * t2

            ve1 - vs * t1 = ve2 - vs * t2
            ve1 - ve2 = vs * t1 - vs * t2
            ve1 - ve2 = vs * (t1 - t2)
            vs = (ve1 - ve2) / (t1 - t2)

            Approximation #1

            0.66 = v0 + vs * 17
            1.22 = v0 + vs * 49

            vs = (0.66 - 1.22) / (17 - 49) = 0.0175
            v0 = 0.66 - 0.0175 * 17 = 0.3625

            Approximation #2

            0.88 = v0 + vs * 26
            1.22 = v0 + vs * 49

            vs = (0.88 - 1.22) / (26 - 49) = 0.0147
            v0 = 0.88 - 0.0147 * 26 = 0.4978

            The two approximations above show that acceleration isn't linear and is affected by a drag:
                26     > 17     - the longer the distance
                0.0147 < 0.0175 - the lesser an assumed linear acceleration
                0.4978 > 0.3625 - the bigger an assumed initial speed

            Finally, it's too hard for now for the author of this plugin to figure out what to do with it. :)

            So, the Fireballs won't be processed, see ProjectileHoming for more details.
        */
        /*
        } else if (projectile instanceof WitherSkull) {
            return new ProjectileMotion(0.0, 0.27);
        } else if (projectile instanceof Fireball) {
            return new ProjectileMotion(0.0, 0.05);
            */
        } else {
            return null;
        }
    }
}