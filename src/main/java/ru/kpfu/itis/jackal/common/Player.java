package ru.kpfu.itis.jackal.common;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class Player {
    private String id;
    private String name;
    private String teamColor;
    private int gold;
    private List<Pirate> pirates;
    private boolean ready;
    private int score;
    private Ship ship;

    public Player() {
        this.pirates = new ArrayList<>();
        this.gold = 0;
        this.ready = false;
        this.score = 0;
    }

    public Player(String id, String name, String teamColor) {
        this();
        this.id = id;
        this.name = name;
        this.teamColor = teamColor;
    }

    public void addPirate(Pirate pirate) {
        pirates.add(pirate);
    }

    public Pirate getPirate(int pirateId) {
        return pirates.stream()
                .filter(p -> p.getId() == pirateId)
                .findFirst()
                .orElse(null);
    }

    public void addGoldToScore(int amount) {
        this.gold += amount;
        this.score += amount;
    }
}