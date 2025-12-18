package ru.kpfu.itis.jackal.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ship {
    private String playerId;
    private int x;
    private int y;
    private int storedGold;

    public Ship() {
        this.storedGold = 0;
    }

    public Ship(String playerId, int x, int y) {
        this();
        this.playerId = playerId;
        this.x = x;
        this.y = y;
    }

    public void addGold(int amount) {
        this.storedGold += amount;
    }

    public int takeGold() {
        int gold = storedGold;
        storedGold = 0;
        return gold;
    }
}