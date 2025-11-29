package ru.kpfu.itis.jackal.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cell {
    private CellType type;
    private Gold gold; // Золото на клетке (может быть null)
    private Pirate pirate; // Пират на клетке (может быть null)

    public Cell() {
        this(CellType.SEA);
    }

    public Cell(CellType type) {
        this.type = type;
        this.gold = null;
        this.pirate = null;
    }

    public Cell(CellType type, Gold gold) {
        this.type = type;
        this.gold = gold;
        this.pirate = null;
    }

    public boolean hasGold() {
        return gold != null;
    }

    public boolean hasPirate() {
        return pirate != null;
    }

    public boolean isPassable() {
        return type != CellType.SEA; // Упрощенно - по морю нельзя ходить
    }
}