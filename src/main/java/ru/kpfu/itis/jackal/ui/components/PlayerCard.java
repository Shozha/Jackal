package ru.kpfu.itis.jackal.ui.components;

import ru.kpfu.itis.jackal.ui.theme.GameTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class PlayerCard extends JPanel {

    private String playerName;
    private int gold;
    private int playerIndex;
    private boolean isReady;
    private boolean isCurrentTurn;

    public PlayerCard(String playerName, int gold, int playerIndex,
                      boolean isReady, boolean isCurrentTurn) {
        this.playerName = playerName;
        this.gold = gold;
        this.playerIndex = playerIndex;
        this.isReady = isReady;
        this.isCurrentTurn = isCurrentTurn;

        setOpaque(false);
        setPreferredSize(new Dimension(280, 100));
        setMaximumSize(new Dimension(280, 100));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        Color playerColor = GameTheme.getPlayerColor(playerIndex);
        Color cardBg = GameTheme.BACKGROUND_TERTIARY;
        Color borderColor = playerColor;

        if (isCurrentTurn) borderColor = playerColor.brighter();

        RoundRectangle2D roundRect = new RoundRectangle2D.Float(
                0, 0, width - 1, height - 1,
                GameTheme.BORDER_RADIUS, GameTheme.BORDER_RADIUS
        );
        g2d.setColor(cardBg);
        g2d.fill(roundRect);

        float borderWidth = isCurrentTurn ? 3f : 2f;
        g2d.setStroke(new BasicStroke(borderWidth));
        g2d.setColor(borderColor);
        g2d.draw(roundRect);

        int colorBoxSize = 80;
        g2d.setColor(playerColor);
        g2d.fillRect(5, 10, colorBoxSize, height - 20);

        g2d.setStroke(new BasicStroke(1f));
        g2d.setColor(GameTheme.BORDER_LIGHT);
        g2d.drawLine(colorBoxSize + 5, 10, colorBoxSize + 5, height - 10);

        int textStartX = colorBoxSize + 15;
        int textStartY = 20;

        g2d.setFont(GameTheme.FONT_HEADING_3);
        g2d.setColor(GameTheme.TEXT_PRIMARY);
        g2d.drawString(playerName, textStartX, textStartY);

        int goldY = textStartY + 25;
        g2d.setFont(GameTheme.FONT_BODY);
        g2d.setColor(GameTheme.TEXT_SECONDARY);
        g2d.drawString("Золото: ", textStartX, goldY);

        g2d.setFont(GameTheme.FONT_HEADING_3);
        g2d.setColor(GameTheme.ACCENT_GOLD);
        g2d.drawString(String.valueOf(gold), textStartX + 95, goldY);

        int statusY = goldY + 22;
        String statusText = isReady ? "Готов" : "Не готов";
        Color statusColor = isReady ? GameTheme.ACCENT_SUCCESS : GameTheme.ACCENT_WARNING;

        g2d.setFont(GameTheme.FONT_SMALL);
        g2d.setColor(statusColor);
        g2d.drawString(statusText, textStartX, statusY);

        if (isCurrentTurn) {
            String turnText = "ВАШ ХОД";
            FontMetrics fm = g2d.getFontMetrics();
            int turnX = width - textStartX - fm.stringWidth(turnText) - 5;

            g2d.setFont(GameTheme.FONT_BODY);
            g2d.setColor(GameTheme.ACCENT_WARNING);
            g2d.drawString(turnText, turnX, textStartY + 8);
        }
    }

    public void updateGold(int gold) {
        this.gold = gold;
        repaint();
    }

    public void setReady(boolean ready) {
        this.isReady = ready;
        repaint();
    }

    public void setCurrentTurn(boolean currentTurn) {
        this.isCurrentTurn = currentTurn;
        repaint();
    }

    public void updatePlayerInfo(String name, int gold, boolean isReady, boolean isCurrentTurn) {
        this.playerName = name;
        this.gold = gold;
        this.isReady = isReady;
        this.isCurrentTurn = isCurrentTurn;
        repaint();
    }
}