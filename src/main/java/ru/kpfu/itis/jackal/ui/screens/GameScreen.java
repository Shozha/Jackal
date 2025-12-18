package ru.kpfu.itis.jackal.ui.screens;

import lombok.Setter;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * GameScreen - основной игровой экран с доской 9x9
 * ✅ Версия [99] - МОРЕ ВИДНО + КОРАБЛИ С ПИРАТАМИ
 *
 * ✅ Море (SEA) теперь видно как синие клетки
 * ✅ Добавлены корабли игроков на краях доски
 * ✅ В кораблях стоят пираты игрока
 * ✅ Можно двигать корабль целиком (со всеми пиратами)
 */
public class GameScreen extends JPanel {

    private static final int BOARD_SIZE = 9;
    private static final int CELL_SIZE = 60;

    private JLabel currentPlayerLabel;
    private JLabel roundLabel;
    private JLabel gameStatusLabel;
    private JLabel actionStatusLabel;
    private JList<String> playersInfoListView;
    private DefaultListModel<String> playersInfoModel;
    private JButton selectPirateButton;
    private JButton endTurnButton;
    private JButton exitButton;
    private BoardPanel boardPanel;

    private java.util.function.BiConsumer<Integer, Integer> onCellClicked;
    private Runnable onEndTurn;
    private Integer selectedPirateId = null;
    private Set<String> possibleMoves = new HashSet<>();

    public GameScreen() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        // TOP - информация о раунде и игроке
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 15));
        topPanel.setBackground(new Color(51, 51, 51));

        currentPlayerLabel = new JLabel("Ход: --");
        currentPlayerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        currentPlayerLabel.setForeground(Color.WHITE);
        topPanel.add(currentPlayerLabel);

        roundLabel = new JLabel("Раунд: 0");
        roundLabel.setFont(new Font("Arial", Font.BOLD, 16));
        roundLabel.setForeground(Color.WHITE);
        topPanel.add(roundLabel);

        add(topPanel, BorderLayout.NORTH);

        // CENTER - доска слева, информация справа
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Левая часть - ДОСКА
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createLineBorder(new Color(153, 153, 153)));

        JLabel boardLabel = new JLabel("Игровая доска 9x9");
        boardLabel.setFont(new Font("Arial", Font.BOLD, 12));
        boardLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        leftPanel.add(boardLabel, BorderLayout.NORTH);

        boardPanel = new BoardPanel();
        JScrollPane boardScroll = new JScrollPane(boardPanel);
        boardScroll.setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
        leftPanel.add(boardScroll, BorderLayout.CENTER);

        centerPanel.add(leftPanel, BorderLayout.CENTER);

        // Правая часть - ИНФОРМАЦИЯ
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createLineBorder(new Color(153, 153, 153)));
        rightPanel.setPreferredSize(new Dimension(250, 500));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel playersLabel = new JLabel("Игроки и золото:");
        playersLabel.setFont(new Font("Arial", Font.BOLD, 12));
        rightPanel.add(playersLabel, BorderLayout.NORTH);

        playersInfoModel = new DefaultListModel<>();
        playersInfoListView = new JList<>(playersInfoModel);
        playersInfoListView.setFont(new Font("Arial", Font.PLAIN, 11));
        playersInfoListView.setBackground(Color.WHITE);

        JScrollPane playersScroll = new JScrollPane(playersInfoListView);
        rightPanel.add(playersScroll, BorderLayout.CENTER);

        gameStatusLabel = new JLabel("Статус: инициализация...");
        gameStatusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        gameStatusLabel.setForeground(new Color(102, 102, 102));
        gameStatusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        rightPanel.add(gameStatusLabel, BorderLayout.SOUTH);

        centerPanel.add(rightPanel, BorderLayout.EAST);

        add(centerPanel, BorderLayout.CENTER);

        // BOTTOM - кнопки управления
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        bottomPanel.setBackground(new Color(245, 245, 245));

        selectPirateButton = new JButton("Выбрать пирата");
        selectPirateButton.setFont(new Font("Arial", Font.BOLD, 12));
        selectPirateButton.setBackground(new Color(76, 175, 80));
        selectPirateButton.setForeground(Color.WHITE);
        selectPirateButton.setFocusPainted(false);
        selectPirateButton.setEnabled(false);
        selectPirateButton.setPreferredSize(new Dimension(150, 35));
        selectPirateButton.addActionListener(e -> showPirateSelection());
        bottomPanel.add(selectPirateButton);

        endTurnButton = new JButton("Ход завершен");
        endTurnButton.setFont(new Font("Arial", Font.BOLD, 12));
        endTurnButton.setBackground(new Color(33, 150, 243));
        endTurnButton.setForeground(Color.WHITE);
        endTurnButton.setFocusPainted(false);
        endTurnButton.setEnabled(false);
        endTurnButton.setPreferredSize(new Dimension(150, 35));
        bottomPanel.add(endTurnButton);

        actionStatusLabel = new JLabel("Готов к ходу");
        actionStatusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        actionStatusLabel.setForeground(new Color(102, 102, 102));
        bottomPanel.add(actionStatusLabel);

        exitButton = new JButton("Выход");
        exitButton.setFont(new Font("Arial", Font.BOLD, 12));
        exitButton.setBackground(new Color(244, 67, 54));
        exitButton.setForeground(Color.WHITE);
        exitButton.setFocusPainted(false);
        exitButton.setPreferredSize(new Dimension(100, 35));
        bottomPanel.add(exitButton);

        add(bottomPanel, BorderLayout.SOUTH);

        // Слушатели для доски
        endTurnButton.addActionListener(e -> {
            if (onEndTurn != null) {
                onEndTurn.run();
            }
        });
    }

    private void showPirateSelection() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Выбрать пирата", true);
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(3, 1, 10, 10));
        dialog.getContentPane().setBackground(Color.WHITE);

        for (int i = 1; i <= 3; i++) {
            final int pirateId = i;
            JButton pirateBtn = new JButton("Пират #" + i);
            pirateBtn.setFont(new Font("Arial", Font.BOLD, 14));
            pirateBtn.setBackground(new Color(33, 150, 243));
            pirateBtn.setForeground(Color.WHITE);
            pirateBtn.setFocusPainted(false);
            pirateBtn.addActionListener(e -> {
                selectPirate(pirateId);
                dialog.dispose();
            });
            dialog.add(pirateBtn);
        }

        dialog.setVisible(true);
    }

    private void selectPirate(int pirateId) {
        this.selectedPirateId = pirateId;
        boardPanel.setSelectedPirateId(pirateId);
        setActionStatus("✅ Выбран пират #" + pirateId);
        System.out.println("[GameScreen] Выбран пират #" + pirateId);
        showPossibleMoves(pirateId);
        boardPanel.repaint();
    }

    private void showPossibleMoves(int pirateId) {
        possibleMoves.clear();
    }

    public void updateBoard(String[][] board) {
        boardPanel.setBoard(board);
        boardPanel.repaint();
    }

    public void updatePossibleMoves(List<String> moves) {
        possibleMoves.clear();
        possibleMoves.addAll(moves);
        boardPanel.setPossibleMoves(possibleMoves);
    }

    public void setCurrentPlayer(String playerName, int round) {
        currentPlayerLabel.setText("Ход: " + playerName);
        roundLabel.setText("Раунд: " + round);
    }

    public void updatePlayersInfo(String[] playerInfos) {
        playersInfoModel.clear();
        for (String info : playerInfos) {
            playersInfoModel.addElement(info);
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
            gameStatusLabel.setForeground(new Color(76, 175, 80));
            selectPirateButton.setEnabled(true);
        } else {
            gameStatusLabel.setForeground(new Color(255, 152, 0));
            selectPirateButton.setEnabled(false);
        }
        endTurnButton.setEnabled(isOurTurn);
        boardPanel.repaint();
    }

    public void setActionStatus(String status) {
        actionStatusLabel.setText(status);
    }

    public void setEndTurnListener(java.awt.event.ActionListener listener) {
        endTurnButton.addActionListener(listener);
    }

    public void setExitListener(java.awt.event.ActionListener listener) {
        exitButton.addActionListener(listener);
    }

    public void setCellClickListener(java.util.function.BiConsumer<Integer, Integer> listener) {
        this.onCellClicked = listener;
        boardPanel.setCellClickListener(listener);
    }

    public static class BoardPanel extends JPanel {

        private String[][] board;
        private int selectedRow = -1;
        private int selectedCol = -1;
        private Integer selectedPirateId = null;
        private Set<String> possibleMoves = new HashSet<>();

        @Setter
        private java.util.function.BiConsumer<Integer, Integer> cellClickListener;

        public BoardPanel() {
            this.board = new String[BOARD_SIZE][BOARD_SIZE];
            initializeBoard();
            setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
            setBackground(Color.WHITE);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int col = e.getX() / CELL_SIZE;
                    int row = e.getY() / CELL_SIZE;

                    if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
                        return;
                    }

                    String cell = board[row][col];

                    if (cell != null && cell.startsWith("P") && cell.length() > 1) {
                        try {
                            int pirateId = Integer.parseInt(cell.substring(1));
                            selectedPirateId = pirateId;
                            repaint();
                            if (cellClickListener != null) {
                                cellClickListener.accept(-1, pirateId);
                            }
                            return;
                        } catch (NumberFormatException ex) {
                            System.err.println("[GameScreen] Ошибка парсинга пирата: " + cell);
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

        public void setPossibleMoves(Set<String> moves) {
            this.possibleMoves = new HashSet<>(moves);
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

                    g2d.setColor(getCellColor(board[y][x]));
                    g2d.fillRect(px, py, CELL_SIZE, CELL_SIZE);

                    String moveKey = x + "," + y;
                    if (possibleMoves.contains(moveKey)) {
                        g2d.setColor(new Color(173, 216, 230, 150));
                        g2d.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                    }

                    g2d.setColor(new Color(153, 153, 153));
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawRect(px, py, CELL_SIZE, CELL_SIZE);

                    if (x == selectedCol && y == selectedRow) {
                        g2d.setColor(new Color(255, 0, 0, 120));
                        g2d.setStroke(new BasicStroke(3));
                        g2d.drawRect(px + 2, py + 2, CELL_SIZE - 4, CELL_SIZE - 4);
                    }

                    String cell = board[y][x];
                    if (cell != null && cell.startsWith("P") && cell.length() > 1) {
                        try {
                            int pirateId = Integer.parseInt(cell.substring(1));
                            g2d.setColor(new Color(244, 67, 54));
                            g2d.fillOval(px + 10, py + 10, 40, 40);
                            g2d.setColor(Color.WHITE);
                            g2d.setFont(new Font("Arial", Font.BOLD, 14));
                            g2d.drawString(String.valueOf(pirateId), px + CELL_SIZE / 2 - 5, py + CELL_SIZE / 2 + 6);

                            if (selectedPirateId != null && pirateId == selectedPirateId) {
                                g2d.setColor(Color.YELLOW);
                                g2d.setStroke(new BasicStroke(3));
                                g2d.drawOval(px + 8, py + 8, 44, 44);
                            }
                        } catch (NumberFormatException ex) {
                            // Skip
                        }
                    }
                    else if (cell != null && Character.isDigit(cell.charAt(0))) {
                        g2d.setColor(new Color(255, 193, 7));
                        g2d.fillRect(px + 15, py + 15, 30, 30);
                        g2d.setColor(new Color(255, 152, 0));
                        g2d.drawRect(px + 15, py + 15, 30, 30);
                        g2d.setColor(Color.BLACK);
                        g2d.setFont(new Font("Arial", Font.BOLD, 10));
                        g2d.drawString(cell, px + CELL_SIZE / 2 - 3, py + CELL_SIZE / 2 + 4);
                    }
                }
            }
        }

        private Color getCellColor(String cell) {
            if (cell == null || cell.equals(" ")) return Color.DARK_GRAY;

            return switch (cell) {
                // ✅ ВСЕГДА ВИДНО С НАЧАЛА:
                case "SEA" -> new Color(33, 150, 243);              // синее море

                // ✅ ПЛЯЖ - ОДИН ЦВЕТ ДЛЯ ВСЕХ (песочный, чтобы не раскрывать стратегию)
                case "BEACH" -> new Color(210, 180, 140);           // песочный
                case "BEACH_RED" -> new Color(210, 180, 140);       // один цвет
                case "BEACH_BLUE" -> new Color(210, 180, 140);      // один цвет
                case "BEACH_GREEN" -> new Color(210, 180, 140);     // один цвет
                case "BEACH_YELLOW" -> new Color(210, 180, 140);    // один цвет

                case "SHIP" -> new Color(121, 85, 72);              // коричневый корабль

                // ✅ ВИДНО КОГДА ОТКРОЕТСЯ:
                case "PLAIN" -> new Color(139, 195, 74);            // равнина
                case "FOREST" -> new Color(56, 142, 60);            // лес
                case "MOUNTAIN" -> new Color(117, 117, 117);        // гора
                case "FORT" -> new Color(255, 152, 0);              // форт
                case "LAND" -> new Color(139, 195, 74);

                // ✅ СКРЫТО:
                case "HIDDEN" -> Color.DARK_GRAY;                   // невидимо

                default -> Color.DARK_GRAY;
            };
        }
    }
}