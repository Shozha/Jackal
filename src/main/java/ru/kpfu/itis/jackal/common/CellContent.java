package ru.kpfu.itis.jackal.common;

import lombok.Getter;

@Getter
public enum CellContent {

    EMPTY("Пусто"),

    GOLD_1("Золото 1"),
    GOLD_2("Золото 2"),
    GOLD_3("Золото 3"),

    TRAP("Ловушка"),

    ARROW_UP("Стрелка вверх"),
    ARROW_DOWN("Стрелка вниз"),
    ARROW_LEFT("Стрелка влево"),
    ARROW_RIGHT("Стрелка вправо"),

    CANNON("Пушка");
    
    private final String displayName;
    
    CellContent(String displayName) {
        this.displayName = displayName;
    }

    public boolean isGold() {
        return this == GOLD_1 || this == GOLD_2 || this == GOLD_3;
    }

    public boolean isArrow() {
        return this == ARROW_UP || this == ARROW_DOWN || 
               this == ARROW_LEFT || this == ARROW_RIGHT;
    }

    public boolean isDangerous() {
        return this == TRAP || this == CANNON || isArrow();
    }
}