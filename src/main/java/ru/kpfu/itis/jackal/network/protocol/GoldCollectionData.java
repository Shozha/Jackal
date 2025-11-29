package ru.kpfu.itis.jackal.network.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoldCollectionData {
    private int pirateId;
    private int goldAmount;
    private int playerGold;
}
