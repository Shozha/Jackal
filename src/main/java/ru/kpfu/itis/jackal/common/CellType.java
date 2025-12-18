package ru.kpfu.itis.jackal.common;

import lombok.Getter;

public enum CellType {

    BEACH("Пляж"),
    SEA("Море"),
    PLAIN("Равнина"),
    FOREST("Лес"),
    MOUNTAIN("Горы"),
    FORT("Форт");

    @Getter
    private final String displayName;

    CellType(String displayName) {
        this.displayName = displayName;
    }

    public boolean isIslandTerrain() {
        return this == PLAIN || this == FOREST || this == MOUNTAIN || this == FORT;
    }

    public boolean isWater() {
        return this == SEA;
    }

    public boolean isBeach() {
        return this == BEACH;
    }
}