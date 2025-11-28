package ru.kpfu.itis.jackal.network.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CombatData {
    private int attackerPirateId;
    private int defenderPirateId;
    private boolean attackerWins;
}
