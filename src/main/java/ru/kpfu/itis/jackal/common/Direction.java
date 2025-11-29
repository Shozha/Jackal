package ru.kpfu.itis.jackal.common;

import lombok.Getter;

@Getter
public enum Direction {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0),
    UP_LEFT(-1, -1),
    UP_RIGHT(1, -1),
    DOWN_LEFT(-1, 1),
    DOWN_RIGHT(1, 1);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public static Direction fromVector(int dx, int dy) {
        for (Direction dir : values()) {
            if (dir.dx == dx && dir.dy == dy) {
                return dir;
            }
        }
        return null;
    }
}