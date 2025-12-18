package ru.kpfu.itis.jackal.common;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

/**
 * Cell - клетка игровой доски
 * ✅ Версия [98] - Gson JSON Serialization
 *
 * ✅ Добавлена сериализация в JsonObject для Gson
 * ✅ Методы для работы с пиратами, золотом, ловушками
 * ✅ Поддержка FOG OF WAR (isRevealed, isVisible)
 */
@Getter
@Setter
public class Cell {
    private CellType type;        // Тип ландшафта
    private CellContent content;  // Содержимое (ловушка, стрелка и т.д.)
    private boolean isRevealed;   // ⭐ Открыта ли клетка?
    private boolean isVisible;    // Видна ли для игрока?
    private Gold gold;            // Золото
    private Pirate pirate;        // Пират (если есть)

    // Конструкторы
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

        // ✅ ЕСЛИ КЛЕТКА НЕ ОТКРЫТА - отправляем только "HIDDEN"
        if (!isRevealed) {
            obj.addProperty("type", "HIDDEN");
            obj.addProperty("isRevealed", false);
            obj.addProperty("isVisible", false);
            return obj;
        }

        // ✅ ЕСЛИ ОТКРЫТА - отправляем полную информацию
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

    // FOG OF WAR методы
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

    // Проходимость
    public boolean isWalkable(boolean carryingGold) {
        // Море ВСЕГДА видно, но не проходимо пешком
        if (type == CellType.SEA) {
            return false;
        }

        // С золотом можно ходить только по открытым плиткам
        if (carryingGold && !isRevealed) {
            return false;
        }

        return true;
    }

    // Золото
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

    // Пираты
    public boolean hasPirate() {
        return pirate != null;
    }

    // Стрелки и ловушки
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