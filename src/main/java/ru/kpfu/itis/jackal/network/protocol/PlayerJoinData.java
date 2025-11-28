package ru.kpfu.itis.jackal.network.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerJoinData {
    private String playerName;
    private String teamColor;
}

