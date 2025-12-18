package ru.kpfu.itis.jackal.dto;

import lombok.Data;

@Data
public class PlayerDto {
    private String id;
    private String name;
    private boolean ready;
    private int score;
}
