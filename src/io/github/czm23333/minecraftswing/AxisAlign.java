package io.github.czm23333.minecraftswing;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Objects;

public class AxisAlign {
    public final Axis axis;
    private final double pos;

    public AxisAlign(Axis axis, double pos) {
        this.axis = Objects.requireNonNull(axis);
        this.pos = pos;
    }

    public Location align(Location org) {
        Location tmp = org.clone();
        Vector vector = tmp.getDirection();
        switch(axis) {
            case X:
                if(vector.getX() == 0.0) return null;
                return tmp.add(vector.multiply((pos - tmp.getX()) / vector.getX()));
            case Y:
                if(vector.getY() == 0.0) return null;
                return tmp.add(vector.multiply((pos - tmp.getY()) / vector.getY()));
            case Z:
                if(vector.getZ() == 0.0) return null;
                return tmp.add(vector.multiply((pos - tmp.getZ()) / vector.getZ()));
            default:
                throw new IllegalStateException("Invalid axis");
        }
    }

    public enum Axis {
        X, Y, Z
    }
}
