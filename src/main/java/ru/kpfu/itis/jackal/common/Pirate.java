package ru.kpfu.itis.jackal.common;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Pirate {

    private int id;
    private int x;
    private int y;
    private int goldCarrying;
    private boolean alive;

    public Pirate() {
        this.alive = true;
        this.goldCarrying = 0;
    }

    public Pirate(int id, int startX, int startY) {
        this();
        this.id = id;
        this.x = startX;
        this.y = startY;
    }

    public void collectGold(int amount) {
        this.goldCarrying += amount;
        System.out.println("Пират #" + id + " собрал " + amount + " золота. Всего: " + this.goldCarrying);
    }

    public int depositGold() {
        int deposited = this.goldCarrying;
        this.goldCarrying = 0;
        System.out.println("Пират #" + id + " сдал " + deposited + " золота");
        return deposited;
    }

    @Override
    public String toString() {
        return "Pirate{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                ", goldCarrying=" + goldCarrying +
                ", alive=" + alive +
                '}';
    }
}
