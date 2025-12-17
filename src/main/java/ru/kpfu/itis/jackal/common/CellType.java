package ru.kpfu.itis.jackal.common;

/**
 * CellType - тип клетки (ландшафт)
 * Версия [96] - ИСПРАВЛЕНО:
 *
 * ✅ УБРАНО: BEACH_RED, BEACH_BLUE, BEACH_GREEN, BEACH_YELLOW
 * ✅ ДОБАВЛЕНО: просто BEACH (пляж - место кораблей)
 * ✅ Отделено от содержимого (используй CellContent)
 */
public enum CellType {

    // ⭐ ПЛЯЖ (береговые клетки, где стоят корабли - углы доски)
    BEACH("Пляж"),

    // ⭐ МОРЕ (вода - пираты не ходят пешком, но корабли плывут)
    SEA("Море"),

    // ⭐ ОСТРОВ - типы ландшафта
    PLAIN("Равнина"),           // Обычная клетка для ходьбы
    FOREST("Лес"),              // Клетка с лесом
    MOUNTAIN("Горы"),           // Клетка с горами

    // ⭐ ФОРТ (центр острова - специальное место)
    FORT("Форт");               // Форт - опасное место (пушка!)

    private final String displayName;

    CellType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Это ландшафт острова? (не пляж, не вода)
     */
    public boolean isIslandTerrain() {
        return this == PLAIN || this == FOREST || this == MOUNTAIN || this == FORT;
    }

    /**
     * Это вода?
     */
    public boolean isWater() {
        return this == SEA;
    }

    /**
     * Это пляж? (берег)
     */
    public boolean isBeach() {
        return this == BEACH;
    }
}