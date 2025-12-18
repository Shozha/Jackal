package ru.kpfu.itis.jackal.ui.theme;

import javax.swing.*;
import java.awt.*;

/**
 * GameTheme - единая тема для всего приложения Jackal
 * Все цвета, шрифты и размеры в одном месте
 */
public class GameTheme {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 🎨 ЦВЕТОВАЯ ПАЛИТРА (Тёмная, как в блокчейн-приложениях)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    // Основные цвета фона
    public static final Color BACKGROUND_PRIMARY = new Color(15, 15, 25);      // Почти чёрный
    public static final Color BACKGROUND_SECONDARY = new Color(25, 25, 40);    // Тёмный серый-синий
    public static final Color BACKGROUND_TERTIARY = new Color(35, 35, 50);     // Светлый серый-синий

    // Текст
    public static final Color TEXT_PRIMARY = new Color(230, 230, 240);         // Почти белый
    public static final Color TEXT_SECONDARY = new Color(150, 150, 170);       // Серый текст
    public static final Color TEXT_DARK = new Color(100, 100, 120);            // Тёмный серый

    // Акцентные цвета
    public static final Color ACCENT_PRIMARY = new Color(100, 200, 255);       // Светло-голубой (основной акцент)
    public static final Color ACCENT_SUCCESS = new Color(76, 175, 80);         // Зелёный (ОК, готов)
    public static final Color ACCENT_WARNING = new Color(255, 152, 0);         // Оранжевый (внимание)
    public static final Color ACCENT_DANGER = new Color(244, 67, 54);          // Красный (ошибка)
    public static final Color ACCENT_GOLD = new Color(255, 193, 7);            // Золото (для золота на доске)

    // Цвета для игроков (4 пирата)
    public static final Color PLAYER_RED = new Color(244, 67, 54);
    public static final Color PLAYER_BLUE = new Color(33, 150, 243);
    public static final Color PLAYER_GREEN = new Color(76, 175, 80);
    public static final Color PLAYER_YELLOW = new Color(255, 193, 7);

    public static final Color[] PLAYER_COLORS = {
            PLAYER_RED, PLAYER_BLUE, PLAYER_GREEN, PLAYER_YELLOW
    };

    // Цвета бордеров и разделителей
    public static final Color BORDER_LIGHT = new Color(60, 60, 80);            // Светлый бордер
    public static final Color BORDER_BRIGHT = new Color(100, 200, 255);        // Яркий бордер (акцент)

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 🔤 ШРИФТЫ
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    public static final String FONT_FAMILY = "Arial";

    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 32);
    public static final Font FONT_HEADING_1 = new Font(FONT_FAMILY, Font.BOLD, 24);
    public static final Font FONT_HEADING_2 = new Font(FONT_FAMILY, Font.BOLD, 18);
    public static final Font FONT_HEADING_3 = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_BUTTON = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 11);

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 📏 РАЗМЕРЫ И ОТСТУПЫ
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    public static final int PADDING_SMALL = 5;
    public static final int PADDING_MEDIUM = 10;
    public static final int PADDING_LARGE = 15;
    public static final int PADDING_XLARGE = 20;

    public static final int BORDER_RADIUS = 8;
    public static final int BORDER_WIDTH = 2;

    public static final int BUTTON_HEIGHT = 40;
    public static final int BUTTON_WIDTH_SMALL = 120;
    public static final int BUTTON_WIDTH_MEDIUM = 160;
    public static final int BUTTON_WIDTH_LARGE = 200;

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 🎯 ПОМОЩНИКИ ДЛЯ СТИЛИЗАЦИИ
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Создать стилизованную кнопку с темой
     */
    public static JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(FONT_BUTTON);
        button.setBackground(bgColor);
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(BUTTON_WIDTH_MEDIUM, BUTTON_HEIGHT));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover эффект (простой - меняем яркость)
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(brighten(bgColor, 20));
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    /**
     * Создать кнопку большого размера
     */
    public static JButton createButtonLarge(String text, Color bgColor) {
        JButton button = createButton(text, bgColor);
        button.setPreferredSize(new Dimension(BUTTON_WIDTH_LARGE, BUTTON_HEIGHT));
        return button;
    }

    /**
     * Создать кнопку маленького размера
     */
    public static JButton createButtonSmall(String text, Color bgColor) {
        JButton button = createButton(text, bgColor);
        button.setPreferredSize(new Dimension(BUTTON_WIDTH_SMALL, BUTTON_HEIGHT));
        button.setFont(FONT_BODY);
        return button;
    }

    /**
     * Применить стиль тёмного фона ко всему контейнеру
     */
    public static void applyDarkTheme(JPanel panel) {
        panel.setBackground(BACKGROUND_PRIMARY);
        panel.setForeground(TEXT_PRIMARY);
    }

    /**
     * Создать красивую разделительную линию
     */
    public static JSeparator createSeparator() {
        JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
        separator.setBackground(BORDER_LIGHT);
        separator.setForeground(BORDER_LIGHT);
        return separator;
    }

    /**
     * Создать вертикальный разделитель
     */
    public static JSeparator createVerticalSeparator() {
        JSeparator separator = new JSeparator(JSeparator.VERTICAL);
        separator.setBackground(BORDER_LIGHT);
        separator.setForeground(BORDER_LIGHT);
        return separator;
    }

    /**
     * Создать подсвеченный текст (для заголовков и важной информации)
     */
    public static JLabel createAccentLabel(String text, Font font) {
        return createLabel(text, font, ACCENT_PRIMARY);
    }

    /**
     * Создать обычный текст
     */
    public static JLabel createLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /**
     * Сделать цвет ярче на N единиц
     */
    private static Color brighten(Color color, int amount) {
        int r = Math.min(color.getRed() + amount, 255);
        int g = Math.min(color.getGreen() + amount, 255);
        int b = Math.min(color.getBlue() + amount, 255);
        return new Color(r, g, b);
    }

    /**
     * Получить цвет для игрока по индексу (0-3)
     */
    public static Color getPlayerColor(int playerIndex) {
        if (playerIndex >= 0 && playerIndex < PLAYER_COLORS.length) {
            return PLAYER_COLORS[playerIndex];
        }
        return ACCENT_PRIMARY;
    }

    /**
     * Получить название цвета
     */
    public static String getPlayerColorName(int playerIndex) {
        String[] colorNames = {"Красный", "Синий", "Зелёный", "Жёлтый"};
        if (playerIndex >= 0 && playerIndex < colorNames.length) {
            return colorNames[playerIndex];
        }
        return "Неизвестный";
    }
}