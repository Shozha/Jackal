package ru.kpfu.itis.jackal.common;

import lombok.Getter;
import lombok.Setter;

/**
 * Cell - клетка игровой доски
 * Версия [96] - ИСПРАВЛЕНО:
 *
 * ✅ Добавлен FOG OF WAR (isRevealed, isVisible)
 * ✅ Содержимое отделено от типа (CellContent)
 * ✅ Правильные типы клеток (BEACH, SEA, PLAIN, FOREST, MOUNTAIN, FORT)
 * ✅ Методы для открытия и видимости
 */
@Getter
@Setter
public class Cell {

    private CellType type;                  // Тип клетки (тип ландшафта)
    private CellContent content;            // Содержимое (золото, ловушка, стрелка и т.д.)

    private boolean isRevealed;             // ⭐ Открыта ли клетка? (видит ли содержимое)
    private boolean isVisible;              // Видна ли для текущего игрока? (видит ли тип)

    private Gold gold;                      // Физическое золото на клетке (если content == GOLD_*)
    private Pirate pirate;                  // Пират на клетке (может быть null)

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
        this.isRevealed = false;            // ⭐ По умолчанию ЗАКРЫТА
        this.isVisible = false;             // По умолчанию НЕ видна
        this.gold = null;
        this.pirate = null;
    }

    // ⭐ НОВЫЕ МЕТОДЫ FOG OF WAR

    /**
     * Открыть клетку - теперь видно содержимое
     */
    public void reveal() {
        this.isRevealed = true;
    }

    /**
     * Сделать клетку видимой для игрока
     */
    public void makeVisible() {
        this.isVisible = true;
    }

    /**
     * Получить содержимое для отображения в UI
     * - Если закрыта → "HIDDEN"
     * - Если открыта → тип содержимого
     */
    public String getDisplayContent() {
        if (!isRevealed) {
            return "HIDDEN";  // Рубашка вниз
        }
        return content.toString();
    }

    /**
     * Может ли пират ходить по этой клетке?
     *
     * @param carryingGold пират несет ли золото?
     * @return true если может ходить
     */
    public boolean isWalkable(boolean carryingGold) {
        // Море НЕ проходимо пешком
        if (type == CellType.SEA) {
            return false;
        }

        // С золотом можно ходить ТОЛЬКО по открытым плиткам!
        if (carryingGold && !isRevealed) {
            return false;
        }

        // Остальные клетки проходимы
        return true;
    }

    /**
     * Может ли пират подобрать золото с этой клетки?
     */
    public boolean canCollectGold() {
        if (!isRevealed) {
            return false;  // Закрытая клетка - не видно что там
        }

        // Если содержимое это золото
        return content == CellContent.GOLD_1 ||
                content == CellContent.GOLD_2 ||
                content == CellContent.GOLD_3;
    }

    /**
     * Сколько золота на этой клетке?
     */
    public int getGoldAmount() {
        return switch (content) {
            case GOLD_1 -> 1;
            case GOLD_2 -> 2;
            case GOLD_3 -> 3;
            default -> 0;
        };
    }

    /**
     * Проверка наличия золота
     */
    public boolean hasGold() {
        return gold != null;
    }

    /**
     * Проверка наличия пирата
     */
    public boolean hasPirate() {
        return pirate != null;
    }

    /**
     * Есть ли ловушка на этой клетке?
     */
    public boolean hasArrow() {
        return content == CellContent.ARROW_UP ||
                content == CellContent.ARROW_DOWN ||
                content == CellContent.ARROW_LEFT ||
                content == CellContent.ARROW_RIGHT;
    }

    /**
     * Есть ли ловушка на этой клетке?
     */
    public boolean hasTrap() {
        return content == CellContent.TRAP;
    }

    /**
     * Получить направление стрелки
     */
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