package com.gmail.uprial.takeaim.fixtures;

import org.bukkit.entity.Fireball;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// https://github.com/PaperMC/Paper/issues/10850
public class FireballAdapter {
    static Method getAccelerationMethod = null;
    static Method setAccelerationMethod = null;

    public static Vector getAcceleration(final Fireball fireball) {
        if(getAccelerationMethod == null) {
            try {
                getAccelerationMethod = fireball.getClass().getMethod("getAcceleration");
            } catch (NoSuchMethodException ignored) {
                try {
                    getAccelerationMethod = fireball.getClass().getMethod("getPower");
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            return (Vector)getAccelerationMethod.invoke(fireball);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setAcceleration(final Fireball fireball, final Vector acceleration) {
        if(setAccelerationMethod == null) {
            try {
                setAccelerationMethod = fireball.getClass().getMethod("setAcceleration", Vector.class);
            } catch (NoSuchMethodException ignored) {
                try {
                    setAccelerationMethod = fireball.getClass().getMethod("setPower", Vector.class);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            setAccelerationMethod.invoke(fireball, acceleration);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
