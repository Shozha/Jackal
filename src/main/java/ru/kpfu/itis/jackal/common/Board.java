package ru.kpfu.itis.jackal.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Board {
    private int width;
    private int height;
    private Cell[][] cells;

    public Board() {
        this(9, 9);
    }

    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        this.cells = new Cell[width][height];
    }

    public Cell getCell(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return cells[x][y];
        }
        return null;
    }

    public void setCell(int x, int y, Cell cell) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            cells[x][y] = cell;
        }
    }

    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }
}