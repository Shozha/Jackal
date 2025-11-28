package ru.kpfu.itis.jackal.network.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MoveActionData {
    private int pirateId;
    private int fromX;
    private int fromY;
    private int toX;
    private int toY;
}
