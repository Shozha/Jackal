package ru.kpfu.itis.jackal.common;

public class GameConfig {
    public static final int BOARD_WIDTH = 9;
    public static final int BOARD_HEIGHT = 9;
    public static final int PIRATES_PER_PLAYER = 3;
    public static final int[] GOLD_VALUES = {1, 2, 3, 5};
    public static final int WINNING_SCORE = 50;

    public static final int[][] SHIP_START_POSITIONS = {
            {0, 0},
            {8, 0},
            {0, 8},
            {8, 8}
    };
}