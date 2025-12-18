package ru.kpfu.itis.jackal.common;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cell {
    private CellType type;
    private CellContent content;
    private boolean isRevealed;
    private boolean isVisible;
    private Gold gold;
    private Pirate pirate;

    public Cell() {
        this(CellType.SEA, CellContent.EMPTY);
    }

    public Cell(CellType type) {
        this(type, CellContent.EMPTY);
    }

    public Cell(CellType type, CellContent content) {
        this.type = type;
        this.content = content;
        this.isRevealed = false;
        this.isVisible = false;
        this.gold = null;
        this.pirate = null;
    }

    public JsonObject toJsonObject() {
        JsonObject obj = new JsonObject();

        if (!isRevealed) {
            obj.addProperty("type", "HIDDEN");
            obj.addProperty("isRevealed", false);
            obj.addProperty("isVisible", false);
            return obj;
        }

        obj.addProperty("type", type != null ? type.toString() : "SEA");
        obj.addProperty("content", content != null ? content.toString() : "EMPTY");
        obj.addProperty("isRevealed", isRevealed);
        obj.addProperty("isVisible", isVisible);

        if (pirate != null) {
            JsonObject pirateObj = new JsonObject();
            pirateObj.addProperty("id", pirate.getId());
            pirateObj.addProperty("x", pirate.getX());
            pirateObj.addProperty("y", pirate.getY());
            pirateObj.addProperty("goldCarrying", pirate.getGoldCarrying());
            obj.add("pirate", pirateObj);
        }

        if (gold != null) {
            obj.addProperty("gold", gold.getAmount());
        }

        return obj;
    }

    public void reveal() {
        this.isRevealed = true;
    }

    public void makeVisible() {
        this.isVisible = true;
    }

    public String getDisplayContent() {
        if (!isRevealed) {
            return "HIDDEN";
        }
        return content.toString();
    }

    public boolean isWalkable(boolean carryingGold) {
        if (type == CellType.SEA) {
            return false;
        }

        if (carryingGold && !isRevealed) {
            return false;
        }

        return true;
    }

    public boolean canCollectGold() {
        if (!isRevealed) {
            return false;
        }

        return content == CellContent.GOLD_1 ||
                content == CellContent.GOLD_2 ||
                content == CellContent.GOLD_3;
    }

    public int getGoldAmount() {
        return switch (content) {
            case GOLD_1 -> 1;
            case GOLD_2 -> 2;
            case GOLD_3 -> 3;
            default -> 0;
        };
    }

    public boolean hasGold() {
        return gold != null;
    }

    public boolean hasPirate() {
        return pirate != null;
    }

    public boolean hasArrow() {
        return content == CellContent.ARROW_UP ||
                content == CellContent.ARROW_DOWN ||
                content == CellContent.ARROW_LEFT ||
                content == CellContent.ARROW_RIGHT;
    }

    public boolean hasTrap() {
        return content == CellContent.TRAP;
    }

    public Direction getArrowDirection() {
        return switch (content) {
            case ARROW_UP -> Direction.UP;
            case ARROW_DOWN -> Direction.DOWN;
            case ARROW_LEFT -> Direction.LEFT;
            case ARROW_RIGHT -> Direction.RIGHT;
            default -> null;
        };
    }
}