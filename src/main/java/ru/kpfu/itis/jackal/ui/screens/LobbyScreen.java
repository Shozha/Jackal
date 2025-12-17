package ru.kpfu.itis.jackal.ui.screens;

import javax.swing.*;
import java.awt.*;

/**
 * LobbyScreen - экран лобби с ожиданием игроков и кнопкой Ready
 * Версия [88] - показывает "(готов)"/"(не готов)" вместо галочек
 */
public class LobbyScreen extends JPanel {

    private JList<String> playersListView;
    private DefaultListModel<String> playersListModel;
    private JLabel playerCountLabel;
    private JLabel statusLabel;
    private JButton readyButton;
    private JButton startGameButton;
    private JButton exitButton;
    private boolean isReady = false;

    public LobbyScreen() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        // TOP - заголовок
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(new Color(51, 51, 51));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);

        JLabel titleLabel = new JLabel("Ожидание подключения других игроков...");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(titleLabel, gbc);

        playerCountLabel = new JLabel("0/4 игроков подключено");
        playerCountLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        playerCountLabel.setForeground(new Color(200, 200, 200));
        gbc.gridy = 1;
        topPanel.add(playerCountLabel, gbc);

        add(topPanel, BorderLayout.NORTH);

        // CENTER - список игроков
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel playersLabel = new JLabel("Подключенные игроки:");
        playersLabel.setFont(new Font("Arial", Font.BOLD, 14));
        centerPanel.add(playersLabel, BorderLayout.NORTH);

        playersListModel = new DefaultListModel<>();
        playersListView = new JList<>(playersListModel);
        playersListView.setFont(new Font("Arial", Font.PLAIN, 12));
        playersListView.setBackground(Color.WHITE);
        playersListView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(playersListView);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(153, 153, 153)));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Ожидание подключения...");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(255, 152, 0));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        centerPanel.add(statusLabel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // BOTTOM - кнопки
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        bottomPanel.setBackground(new Color(245, 245, 245));

        readyButton = new JButton("Готов");
        readyButton.setFont(new Font("Arial", Font.BOLD, 14));
        readyButton.setBackground(new Color(255, 152, 0));
        readyButton.setForeground(Color.WHITE);
        readyButton.setFocusPainted(false);
        readyButton.setPreferredSize(new Dimension(120, 40));
        bottomPanel.add(readyButton);

        startGameButton = new JButton("Начать игру");
        startGameButton.setFont(new Font("Arial", Font.BOLD, 14));
        startGameButton.setBackground(new Color(76, 175, 80));
        startGameButton.setForeground(Color.WHITE);
        startGameButton.setFocusPainted(false);
        startGameButton.setEnabled(false);
        startGameButton.setPreferredSize(new Dimension(180, 40));
        bottomPanel.add(startGameButton);

        exitButton = new JButton("Выход");
        exitButton.setFont(new Font("Arial", Font.BOLD, 14));
        exitButton.setBackground(new Color(244, 67, 54));
        exitButton.setForeground(Color.WHITE);
        exitButton.setFocusPainted(false);
        exitButton.setPreferredSize(new Dimension(120, 40));
        bottomPanel.add(exitButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void addPlayer(String playerName) {
        if (!playersListModel.contains("• " + playerName)) {
            playersListModel.addElement("• " + playerName);
        }
    }

    public void setPlayerCount(int current, int max) {
        playerCountLabel.setText(current + "/" + max + " игроков подключено");
    }

    public void updatePlayersList(String[] players) {
        playersListModel.clear();
        for (String player : players) {
            playersListModel.addElement("• " + player);
        }
    }

    /**
     * ⭐ НОВОЕ: показываем "(готов)"/"(не готов)" вместо галочек
     */
    public void updatePlayersWithReadyStatus(String[] players, boolean[] readyStatus) {
        playersListModel.clear();
        for (int i = 0; i < players.length; i++) {
            String status = (readyStatus != null && i < readyStatus.length && readyStatus[i])
                    ? " (готов)"
                    : " (не готов)";
            playersListModel.addElement("• " + players[i] + status);
        }
        playersListView.repaint();
    }

    /**
     * ⭐ Явное отключение кнопки "Начать игру"
     */
    public void setStatus(String status, boolean allReady) {
        statusLabel.setText(status);
        if (allReady) {
            statusLabel.setForeground(new Color(76, 175, 80));
            startGameButton.setEnabled(true);
        } else {
            statusLabel.setForeground(new Color(255, 152, 0));
            startGameButton.setEnabled(false);
        }
    }

    /**
     * Обновляем статус кнопки Ready
     */
    public void setReadyButtonStatus(boolean ready) {
        this.isReady = ready;
        if (ready) {
            readyButton.setText("Отменить");
            readyButton.setBackground(new Color(76, 175, 80));
        } else {
            readyButton.setText("Готов");
            readyButton.setBackground(new Color(255, 152, 0));
        }
    }

    public boolean getReadyStatus() {
        return isReady;
    }

    public void setReadyListener(java.awt.event.ActionListener listener) {
        readyButton.addActionListener(listener);
    }

    public void setStartGameListener(java.awt.event.ActionListener listener) {
        startGameButton.addActionListener(listener);
    }

    public void setExitListener(java.awt.event.ActionListener listener) {
        exitButton.addActionListener(listener);
    }
}
