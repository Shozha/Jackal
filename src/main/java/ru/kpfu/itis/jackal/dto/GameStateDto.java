package ru.kpfu.itis.jackal.dto;

import java.util.ArrayList;
import java.util.List;

public class GameStateDto {
    public List<PlayerDto> players = new ArrayList<>();
    public String currentPlayerId;
    public int turnNumber;
    public String[][] board;
}
