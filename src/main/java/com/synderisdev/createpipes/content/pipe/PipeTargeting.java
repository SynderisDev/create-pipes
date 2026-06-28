package com.synderisdev.createpipes.content.pipe;

import net.minecraft.core.Direction;

public final class PipeTargeting {
    private static final double CORE_MIN = 5.0D / 16.0D;
    private static final double CORE_MAX = 11.0D / 16.0D;
    private static final double CENTER = 0.5D;

    private PipeTargeting() {
    }

    public static Direction sideFromHit(double x, double y, double z, Direction fallback) {
        Direction armSide = armSideFromHit(x, y, z);
        if (armSide != null) {
            return armSide;
        }

        Direction nearest = fallback;
        double nearestDistance = 0;

        double xDistance = Math.abs(x - CENTER);
        if (xDistance > nearestDistance) {
            nearestDistance = xDistance;
            nearest = x < CENTER ? Direction.WEST : Direction.EAST;
        }

        double yDistance = Math.abs(y - CENTER);
        if (yDistance > nearestDistance) {
            nearestDistance = yDistance;
            nearest = y < CENTER ? Direction.DOWN : Direction.UP;
        }

        double zDistance = Math.abs(z - CENTER);
        if (zDistance > nearestDistance) {
            nearest = z < CENTER ? Direction.NORTH : Direction.SOUTH;
        }

        return nearest;
    }

    private static Direction armSideFromHit(double x, double y, double z) {
        Direction best = null;
        double bestScore = 0;

        best = score(Direction.WEST, CORE_MIN - x, best, bestScore);
        bestScore = Math.max(bestScore, CORE_MIN - x);
        best = score(Direction.EAST, x - CORE_MAX, best, bestScore);
        bestScore = Math.max(bestScore, x - CORE_MAX);
        best = score(Direction.DOWN, CORE_MIN - y, best, bestScore);
        bestScore = Math.max(bestScore, CORE_MIN - y);
        best = score(Direction.UP, y - CORE_MAX, best, bestScore);
        bestScore = Math.max(bestScore, y - CORE_MAX);
        best = score(Direction.NORTH, CORE_MIN - z, best, bestScore);
        bestScore = Math.max(bestScore, CORE_MIN - z);
        best = score(Direction.SOUTH, z - CORE_MAX, best, bestScore);

        return best;
    }

    private static Direction score(Direction candidate, double score, Direction best, double bestScore) {
        return score > bestScore ? candidate : best;
    }
}
