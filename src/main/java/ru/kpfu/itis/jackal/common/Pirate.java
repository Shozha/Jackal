package ru.kpfu.itis.jackal.common;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Pirate {
    private int id;
    private int x;
    private int y;
    private int goldCarrying; // количество золота, которое несет
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

}
