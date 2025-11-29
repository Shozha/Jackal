package ru.kpfu.itis.jackal.common;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class GameState {
    private Board board;
    private List<Player> players;
    private String currentPlayerId;
    private String winnerPlayerId;
    private int turnNumber;
    private boolean gameStarted;
    private boolean gameFinished;

    public GameState() {
        this.players = new ArrayList<>();
        this.turnNumber = 0;
        this.gameStarted = false;
        this.gameFinished = false;
    }

    // Вспомогательные методы
    public void addPlayer(Player player) {
        players.add(player);
    }

    public Player getPlayer(String playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public Player getWinner() {
        if (winnerPlayerId != null) {
            return getPlayer(winnerPlayerId);
        }
        return null;
    }
}
