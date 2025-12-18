package ru.kpfu.itis.jackal.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class GameStateDto {
    private List<PlayerDto> players = new ArrayList<>();
    private String currentPlayerId;
    private int turnNumber;
    private String[][] board;
}
