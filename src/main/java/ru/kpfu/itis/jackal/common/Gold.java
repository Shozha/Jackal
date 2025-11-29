package ru.kpfu.itis.jackal.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Gold {
    private int amount; // 1, 2, 3, 5 монет
    private int x; // Позиция на поле
    private int y;

    public Gold() {
        this(1, 0, 0);
    }

    public Gold(int amount, int x, int y) {
        this.amount = amount;
        this.x = x;
        this.y = y;
    }
}