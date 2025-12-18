package ru.kpfu.itis.jackal.dto;

import lombok.Data;

@Data
public class CellDto {
    private String type;
    private String content;
    private PirateDto pirate;
    private GoldDto gold;
}
