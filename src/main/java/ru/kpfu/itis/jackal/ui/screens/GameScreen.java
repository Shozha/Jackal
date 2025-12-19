package ru.kpfu.itis.jackal.ui.screens;

import lombok.Setter;
import ru.kpfu.itis.jackal.ui.components.PlayerCard;
import ru.kpfu.itis.jackal.ui.theme.GameTheme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GameScreen extends JPanel {
    private static final int BOARD_SIZE = 9;
    private static final int CELL_SIZE = 60;

    private JLabel currentPlayerLabel;
    private JLabel roundLabel;
    private JLabel gameStatusLabel;
    private JLabel actionStatusLabel;
    private PlayerCard[] playerCards;
    private JPanel playersPanel;
    private JButton selectPirateButton;
    private JButton endTurnButton;
    private JButton exitButton;
    private BoardPanel boardPanel;
    private BiConsumer onCellClicked;
    private Runnable onEndTurn;
    private Integer selectedPirateId = null;
    private Set possibleMoves = new HashSet<>();
    private Map pirateColors = new HashMap<>();
    private JTextArea eventLog;

    public GameScreen() {
        setLayout(new BorderLayout());
        GameTheme.applyDarkTheme(this);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 15));
        topPanel.setBackground(GameTheme.BACKGROUND_SECONDARY);
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, GameTheme.BORDER_BRIGHT));
        currentPlayerLabel = GameTheme.createAccentLabel("Ход: --", GameTheme.FONT_HEADING_2);
        topPanel.add(currentPlayerLabel);
        JSeparator sep1 = GameTheme.createVerticalSeparator();
        sep1.setPreferredSize(new Dimension(2, 20));
        topPanel.add(sep1);
        roundLabel = GameTheme.createLabel("Раунд: 0", GameTheme.FONT_HEADING_2, GameTheme.ACCENT_GOLD);
        topPanel.add(roundLabel);
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GameTheme.applyDarkTheme(centerPanel);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createLineBorder(GameTheme.BORDER_BRIGHT, 3));
        GameTheme.applyDarkTheme(leftPanel);

        JLabel boardLabel = GameTheme.createAccentLabel("Остров Сокровищ 9x9", GameTheme.FONT_HEADING_3);
        boardLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        leftPanel.add(boardLabel, BorderLayout.NORTH);

        JSplitPane boardLogSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        boardLogSplit.setOneTouchExpandable(true);
        boardLogSplit.setDividerLocation(540);

        boardPanel = new BoardPanel();
        JPanel boardWrapper = new JPanel(new BorderLayout());
        GameTheme.applyDarkTheme(boardWrapper);
        boardWrapper.add(boardPanel, BorderLayout.CENTER);
        boardLogSplit.setTopComponent(boardWrapper);

        eventLog = new JTextArea();
        eventLog.setEditable(false);
        eventLog.setLineWrap(true);
        eventLog.setWrapStyleWord(true);
        eventLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        eventLog.setForeground(GameTheme.TEXT_SECONDARY);
        eventLog.setBackground(GameTheme.BACKGROUND_TERTIARY);
        eventLog.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane logScroll = new JScrollPane(eventLog);
        logScroll.setBorder(BorderFactory.createLineBorder(GameTheme.BORDER_LIGHT, 1));

        JPanel logPanel = new JPanel(new BorderLayout());
        JLabel logLabel = GameTheme.createLabel("[LOG] Events:", GameTheme.FONT_SMALL, GameTheme.ACCENT_GOLD);
        logLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        logPanel.add(logLabel, BorderLayout.NORTH);
        logPanel.add(logScroll, BorderLayout.CENTER);
        GameTheme.applyDarkTheme(logPanel);

        boardLogSplit.setBottomComponent(logPanel);
        leftPanel.add(boardLogSplit, BorderLayout.CENTER);
        centerPanel.add(leftPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(320, 500));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GameTheme.applyDarkTheme(rightPanel);

        JLabel playersLabel = GameTheme.createLabel("Игроки и золото:", GameTheme.FONT_HEADING_3, GameTheme.ACCENT_GOLD);
        playersLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        rightPanel.add(playersLabel, BorderLayout.NORTH);

        playersPanel = new JPanel();
        playersPanel.setLayout(new BoxLayout(playersPanel, BoxLayout.Y_AXIS));
        GameTheme.applyDarkTheme(playersPanel);
        JScrollPane playersScroll = new JScrollPane(playersPanel);
        playersScroll.getViewport().setBackground(GameTheme.BACKGROUND_TERTIARY);
        playersScroll.setBorder(BorderFactory.createLineBorder(GameTheme.BORDER_LIGHT, 1));
        rightPanel.add(playersScroll, BorderLayout.CENTER);

        gameStatusLabel = GameTheme.createLabel("Инициализация...", GameTheme.FONT_SMALL, GameTheme.TEXT_SECONDARY);
        gameStatusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        rightPanel.add(gameStatusLabel, BorderLayout.SOUTH);

        centerPanel.add(rightPanel, BorderLayout.EAST);
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        bottomPanel.setBackground(GameTheme.BACKGROUND_SECONDARY);
        bottomPanel.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, GameTheme.BORDER_BRIGHT));

        selectPirateButton = GameTheme.createButton("Выбрать пирата", GameTheme.ACCENT_SUCCESS);
        selectPirateButton.setEnabled(false);
        selectPirateButton.addActionListener(e -> showPirateSelection());
        bottomPanel.add(selectPirateButton);

        endTurnButton = GameTheme.createButton("Ход завершен", GameTheme.ACCENT_PRIMARY);
        endTurnButton.setEnabled(false);
        bottomPanel.add(endTurnButton);

        actionStatusLabel = GameTheme.createLabel("Готов к ходу", GameTheme.FONT_BODY, GameTheme.ACCENT_SUCCESS);
        bottomPanel.add(actionStatusLabel);

        exitButton = GameTheme.createButton("Выход", GameTheme.ACCENT_DANGER);
        bottomPanel.add(exitButton);
        add(bottomPanel, BorderLayout.SOUTH);

        endTurnButton.addActionListener(e -> {
            if (onEndTurn != null) {
                onEndTurn.run();
            }
        });

        playerCards = new PlayerCard[4];
        for (int i = 0; i < 4; i++) {
            PlayerCard card = new PlayerCard("Игрок " + (i + 1), 0, i, false, false);
            playerCards[i] = card;
            playersPanel.add(card);
            playersPanel.add(Box.createVerticalStrut(10));
        }
    }

    public void updatePlayerInfo(int playerIndex, String playerName, int gold, boolean isReady, boolean isCurrentTurn) {
        if (playerIndex >= 0 && playerIndex < playerCards.length) {
            playerCards[playerIndex].updatePlayerInfo(playerName, gold, isReady, isCurrentTurn);
        }
    }

    public void updateAllPlayersInfo(String[] playerInfos) {
        for (int i = 0; i < Math.min(playerInfos.length, playerCards.length); i++) {
            String info = playerInfos[i];
            playerCards[i].updatePlayerInfo("Игрок " + (i + 1), 100, true, false);
        }
    }

    public void updateBoard(String[][] board) {
        boardPanel.setBoard(board);
        boardPanel.repaint();
    }

    public void setPirateColors(Map colors) {
        this.pirateColors.clear();
        this.pirateColors.putAll(colors);
        boardPanel.setPirateColors(colors);
    }

    public void updatePossibleMoves(List moves) {
        possibleMoves.clear();
        possibleMoves.addAll(moves);
        boardPanel.setPossibleMoves(possibleMoves);
    }

    public void setCurrentPlayer(String playerName, int round) {
        currentPlayerLabel.setText("Ход: " + playerName);
        roundLabel.setText("Раунд: " + round);
        for (PlayerCard playerCard : playerCards) {
            playerCard.setCurrentTurn(false);
        }
    }

    public void setSelectedPirate(int pirateId) {
        selectPirate(pirateId);
    }

    public void setGameStatus(String status, boolean isOurTurn) {
        gameStatusLabel.setText(status);
        selectedPirateId = null;
        possibleMoves.clear();
        boardPanel.setSelectedPirateId(null);
        boardPanel.setPossibleMoves(possibleMoves);
        if (isOurTurn) {
            gameStatusLabel.setForeground(GameTheme.ACCENT_SUCCESS);
            selectPirateButton.setEnabled(true);
        } else {
            gameStatusLabel.setForeground(GameTheme.ACCENT_WARNING);
            selectPirateButton.setEnabled(false);
        }
        endTurnButton.setEnabled(isOurTurn);
        boardPanel.repaint();
    }

    public void setActionStatus(String status) {
        actionStatusLabel.setText(status);
    }

    public void setEndTurnListener(ActionListener listener) {
        endTurnButton.addActionListener(listener);
    }

    public void setExitListener(ActionListener listener) {
        exitButton.addActionListener(listener);
    }

    public void setCellClickListener(BiConsumer listener) {
        this.onCellClicked = listener;
        boardPanel.setCellClickListener(listener);
    }

    public void addLog(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String logEntry = String.format("[%s] %s\n", timestamp, message);
        eventLog.append(logEntry);
        eventLog.setCaretPosition(eventLog.getDocument().getLength());
    }

    public void clearLog() {
        eventLog.setText("");
    }

    private void showPirateSelection() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Выбрать пирата", true);
        dialog.setSize(320, 220);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(3, 1, 15, 15));
        JPanel contentPane = (JPanel) dialog.getContentPane();
        GameTheme.applyDarkTheme(contentPane);
        contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        for (int i = 1; i <= 3; i++) {
            final int pirateId = i;
            JButton pirateBtn = GameTheme.createButtonLarge("Пират #" + i, GameTheme.ACCENT_PRIMARY);
            pirateBtn.addActionListener(e -> {
                selectPirate(pirateId);
                dialog.dispose();
            });
            contentPane.add(pirateBtn);
        }
        dialog.setVisible(true);
    }

    private void selectPirate(int pirateId) {
        this.selectedPirateId = pirateId;
        boardPanel.setSelectedPirateId(pirateId);
        setActionStatus("Выбран пират #" + pirateId);
        System.out.println("[GameScreen] Выбран пират #" + pirateId);
        boardPanel.repaint();
    }

    public static class BoardPanel extends JPanel {
        private String[][] board;
        private int selectedRow = -1;
        private int selectedCol = -1;
        private Integer selectedPirateId = null;
        private Set possibleMoves = new HashSet<>();
        private Map pirateColors = new HashMap<>();

        @Setter
        private BiConsumer cellClickListener;

        public BoardPanel() {
            this.board = new String[BOARD_SIZE][BOARD_SIZE];
            initializeBoard();
            setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
            GameTheme.applyDarkTheme(this);
            setMinimumSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int col = e.getX() / CELL_SIZE;
                    int row = e.getY() / CELL_SIZE;
                    if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
                        return;
                    }
                    String cell = board[row][col];
                    if (cell != null && cell.startsWith("P") && cell.length() == 2) {
                        try {
                            int pirateId = Integer.parseInt(cell.substring(1));
                            selectedPirateId = pirateId;
                            repaint();
                            if (cellClickListener != null) {
                                cellClickListener.accept(-1, pirateId);
                            }
                            return;
                        } catch (NumberFormatException ex) {
                            System.err.println("[GameScreen] ❌ Ошибка парсинга пирата. Значение: '" + cell +
                                    "', Подстрока: '" + cell.substring(1) + "'");
                            ex.printStackTrace();
                        }
                    }
                    String moveKey = col + "," + row;
                    if (selectedPirateId != null && possibleMoves.contains(moveKey)) {
                        selectedCol = col;
                        selectedRow = row;
                        repaint();
                        if (cellClickListener != null) {
                            cellClickListener.accept(col, row);
                        }
                        return;
                    }
                    if (selectedPirateId != null && cellClickListener != null) {
                        selectedCol = col;
                        selectedRow = row;
                        repaint();
                        cellClickListener.accept(col, row);
                    }
                }
            });
        }

        private void initializeBoard() {
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    board[i][j] = " ";
                }
            }
        }

        public void setBoard(String[][] newBoard) {
            if (newBoard != null && newBoard.length == BOARD_SIZE) {
                for (int i = 0; i < BOARD_SIZE; i++) {
                    if (newBoard[i] != null && newBoard[i].length == BOARD_SIZE) {
                        System.arraycopy(newBoard[i], 0, board[i], 0, BOARD_SIZE);
                    }
                }
            }
        }

        public void setSelectedPirateId(Integer pirateId) {
            this.selectedPirateId = pirateId;
            repaint();
        }

        public void setPossibleMoves(Set moves) {
            this.possibleMoves = new HashSet<>(moves);
        }

        public void setPirateColors(Map colors) {
            this.pirateColors.clear();
            this.pirateColors.putAll(colors);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int y = 0; y < BOARD_SIZE; y++) {
                for (int x = 0; x < BOARD_SIZE; x++) {
                    int px = x * CELL_SIZE;
                    int py = y * CELL_SIZE;
                    drawCellWithStyle(g2d, px, py, board[y][x]);
                    String moveKey = x + "," + y;
                    if (possibleMoves.contains(moveKey)) {
                        g2d.setColor(new Color(100, 200, 255, 80));
                        g2d.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                    }
                    if (x == selectedCol && y == selectedRow) {
                        g2d.setColor(new Color(255, 100, 100));
                        g2d.setStroke(new BasicStroke(3));
                        g2d.drawRect(px + 2, py + 2, CELL_SIZE - 4, CELL_SIZE - 4);
                    }
                    String cell = board[y][x];
                    if (cell != null && cell.startsWith("P") && cell.length() > 1) {
                        drawPirate(g2d, px, py, cell);
                    } else if (cell != null && Character.isDigit(cell.charAt(0))) {
                        drawGold(g2d, px, py, cell);
                    }
                }
            }
        }

        private void drawCellWithStyle(Graphics2D g2d, int px, int py, String cellType) {
            Color baseColor = getCellColor(cellType);
            GradientPaint gradient = new GradientPaint(px, py, baseColor.brighter(), px, py + CELL_SIZE, baseColor.darker());
            g2d.setPaint(gradient);
            g2d.fillRect(px, py, CELL_SIZE, CELL_SIZE);
            drawPattern(g2d, px, py, cellType);
            g2d.setColor(new Color(0, 0, 0, 40));
            g2d.fillRect(px + CELL_SIZE - 3, py + 3, 3, CELL_SIZE);
            g2d.fillRect(px + 3, py + CELL_SIZE - 3, CELL_SIZE - 3, 3);
            g2d.setColor(new Color(100, 100, 120, 150));
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRect(px, py, CELL_SIZE, CELL_SIZE);
        }

        private void drawPattern(Graphics2D g2d, int px, int py, String cellType) {
            if (cellType == null || cellType.equals(" ")) return;
            g2d.setColor(new Color(255, 255, 255, 15));
            switch (cellType) {
                case "FOREST":
                    for (int i = 0; i < CELL_SIZE; i += 8) {
                        g2d.drawLine(px + i, py, px + i, py + CELL_SIZE);
                    }
                    for (int i = 4; i < CELL_SIZE; i += 8) {
                        g2d.drawLine(px + i, py, px + i, py + CELL_SIZE);
                    }
                    break;
                case "MOUNTAIN":
                    for (int i = -CELL_SIZE; i < CELL_SIZE * 2; i += 8) {
                        g2d.drawLine(px + i, py, px + i + CELL_SIZE, py + CELL_SIZE);
                    }
                    break;
                case "SEA":
                    for (int i = 0; i < CELL_SIZE + CELL_SIZE; i += 6) {
                        g2d.drawLine(px + i, py, px + i - CELL_SIZE, py + CELL_SIZE);
                        g2d.drawLine(px + i + 3, py, px + i + 3 - CELL_SIZE, py + CELL_SIZE);
                    }
                    break;
                case "PLAIN":
                    for (int i = 10; i < CELL_SIZE; i += 12) {
                        for (int j = 10; j < CELL_SIZE; j += 12) {
                            g2d.fillOval(px + i, py + j, 2, 2);
                        }
                    }
                    break;
                case "BEACH":
                    for (int i = 5; i < CELL_SIZE; i += 10) {
                        for (int j = 5; j < CELL_SIZE; j += 10) {
                            g2d.fillRect(px + i, py + j, 3, 3);
                        }
                    }
                    break;
                case "HIDDEN":
                    g2d.drawLine(px, py, px + CELL_SIZE, py + CELL_SIZE);
                    g2d.drawLine(px + CELL_SIZE, py, px, py + CELL_SIZE);
                    break;
            }
        }

        private void drawPirate(Graphics2D g2d, int px, int py, String cell) {
            try {
                int pirateId = Integer.parseInt(cell.substring(1));
                Color pirateColor = (Color) pirateColors.getOrDefault(pirateId, new Color(220, 50, 50));
                g2d.setColor(pirateColor);
                g2d.fillOval(px + 10, py + 10, 40, 40);
                if (selectedPirateId != null && pirateId == selectedPirateId) {
                    g2d.setColor(new Color(255, 255, 100));
                    g2d.setStroke(new BasicStroke(3));
                    g2d.drawOval(px + 8, py + 8, 44, 44);
                    g2d.setColor(new Color(255, 200, 0, 80));
                    g2d.fillOval(px + 12, py + 12, 36, 36);
                }
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                FontMetrics fm = g2d.getFontMetrics();
                String text = String.valueOf(pirateId);
                int textX = px + CELL_SIZE / 2 - fm.stringWidth(text) / 2;
                int textY = py + CELL_SIZE / 2 + fm.getAscent() / 2 - 2;
                g2d.drawString(text, textX, textY);
            } catch (NumberFormatException ex) {}
        }

        private void drawGold(Graphics2D g2d, int px, int py, String amount) {
            GradientPaint goldGradient = new GradientPaint(px + 15, py + 15, new Color(255, 235, 59), px + 45, py + 45, new Color(255, 193, 7));
            g2d.setPaint(goldGradient);
            g2d.fillRect(px + 15, py + 15, 30, 30);
            g2d.setColor(new Color(255, 152, 0));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(px + 15, py + 15, 30, 30);
            g2d.setColor(new Color(0, 0, 0, 30));
            g2d.fillRect(px + 17, py + 42, 26, 3);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 11));
            FontMetrics fm = g2d.getFontMetrics();
            int textX = px + CELL_SIZE / 2 - fm.stringWidth(amount) / 2;
            int textY = py + CELL_SIZE / 2 + fm.getAscent() / 2;
            g2d.drawString(amount, textX, textY);
        }

        private Color getCellColor(String cell) {
            if (cell == null || cell.equals(" ")) {
                return new Color(200, 180, 150);
            }
            return switch (cell) {
                case "SEA" -> new Color(30, 140, 200);
                case "BEACH" -> new Color(220, 200, 120);
                case "SHIP" -> new Color(140, 100, 60);
                case "PLAIN" -> new Color(120, 180, 70);
                case "FOREST" -> new Color(40, 130, 50);
                case "MOUNTAIN" -> new Color(120, 120, 130);
                case "FORT" -> new Color(230, 140, 30);
                case "LAND" -> new Color(150, 180, 100);
                case "HIDDEN" -> new Color(180, 160, 140);
                default -> new Color(180, 180, 180);
            };
        }
    }
}