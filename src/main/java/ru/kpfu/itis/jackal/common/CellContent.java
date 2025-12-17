package ru.kpfu.itis.jackal.common;

import lombok.Getter;

/**
 * CellContent - содержимое клетки (скрыто под рубашкой)
 * Версия [96] - НОВЫЙ:
 * 
 * ⭐ Отделено от типа клетки (CellType)
 * ⭐ Кодирует: золото, ловушки, стрелки, пушку
 */
@Getter
public enum CellContent {
    
    // ⭐ ПУСТО
    EMPTY("Пусто"),
    
    // ⭐ ЗОЛОТО (количество монет)
    GOLD_1("Золото 1"),
    GOLD_2("Золото 2"),
    GOLD_3("Золото 3"),
    
    // ⭐ ЛОВУШКА (пират возвращается на корабль)
    TRAP("Ловушка"),
    
    // ⭐ СТРЕЛКИ (толкают пирата в направлении)
    ARROW_UP("Стрелка вверх"),
    ARROW_DOWN("Стрелка вниз"),
    ARROW_LEFT("Стрелка влево"),
    ARROW_RIGHT("Стрелка вправо"),
    
    // ⭐ ПУШКА (в центральном форте - может стрелять)
    CANNON("Пушка");
    
    private final String displayName;
    
    CellContent(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Это золото?
     */
    public boolean isGold() {
        return this == GOLD_1 || this == GOLD_2 || this == GOLD_3;
    }
    
    /**
     * Это стрелка?
     */
    public boolean isArrow() {
        return this == ARROW_UP || this == ARROW_DOWN || 
               this == ARROW_LEFT || this == ARROW_RIGHT;
    }
    
    /**
     * Это опасный эффект? (ловушка, стрелка, пушка)
     */
    public boolean isDangerous() {
        return this == TRAP || this == CANNON || isArrow();
    }
}