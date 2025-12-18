package ru.kpfu.itis.jackal.ui.screens;

import ru.kpfu.itis.jackal.ui.theme.GameTheme;
import ru.kpfu.itis.jackal.ui.components.PlayerCard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class LobbyScreen extends JPanel {

    private JLabel playerCountLabel;
    private JLabel statusLabel;
    private JButton readyButton;
    private JButton startGameButton;
    private JButton exitButton;
    private JPanel playersPanel;
    private Map<Integer, PlayerCard> playerCards = new HashMap<>();

    private boolean isReady = false;

    public LobbyScreen() {
        setLayout(new BorderLayout());
        GameTheme.applyDarkTheme(this);

        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(GameTheme.BACKGROUND_SECONDARY);
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, GameTheme.BORDER_BRIGHT));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(GameTheme.PADDING_XLARGE, GameTheme.PADDING_XLARGE,
                GameTheme.PADDING_XLARGE, GameTheme.PADDING_XLARGE);

        JLabel titleLabel = GameTheme.createAccentLabel("Ожидание других игроков...", GameTheme.FONT_HEADING_2);
        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(titleLabel, gbc);

        playerCountLabel = GameTheme.createLabel("0/4 игроков", GameTheme.FONT_BODY, GameTheme.TEXT_SECONDARY);
        gbc.gridy = 1;
        topPanel.add(playerCountLabel, gbc);

        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(GameTheme.BACKGROUND_PRIMARY);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(
                GameTheme.PADDING_XLARGE, GameTheme.PADDING_XLARGE,
                GameTheme.PADDING_XLARGE, GameTheme.PADDING_XLARGE
        ));

        JLabel playersLabel = GameTheme.createAccentLabel("Подключённые игроки:", GameTheme.FONT_HEADING_3);
        centerPanel.add(playersLabel, BorderLayout.NORTH);

        playersPanel = new JPanel();
        playersPanel.setLayout(new BoxLayout(playersPanel, BoxLayout.Y_AXIS));
        playersPanel.setBackground(GameTheme.BACKGROUND_PRIMARY);
        playersPanel.setBorder(BorderFactory.createEmptyBorder(
                GameTheme.PADDING_LARGE, 0, GameTheme.PADDING_LARGE, 0
        ));

        JScrollPane scrollPane = new JScrollPane(playersPanel);
        scrollPane.setBackground(GameTheme.BACKGROUND_PRIMARY);
        scrollPane.getViewport().setBackground(GameTheme.BACKGROUND_PRIMARY);
        scrollPane.setBorder(BorderFactory.createLineBorder(GameTheme.BORDER_LIGHT, 1));
        scrollPane.getVerticalScrollBar().setBackground(GameTheme.BACKGROUND_SECONDARY);

        centerPanel.add(scrollPane, BorderLayout.CENTER);

        statusLabel = GameTheme.createLabel("Ожидание подключения...", GameTheme.FONT_BODY, GameTheme.TEXT_SECONDARY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(GameTheme.PADDING_MEDIUM, 0, 0, 0));
        centerPanel.add(statusLabel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, GameTheme.PADDING_XLARGE, GameTheme.PADDING_XLARGE));
        bottomPanel.setBackground(GameTheme.BACKGROUND_SECONDARY);
        bottomPanel.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, GameTheme.BORDER_BRIGHT));

        readyButton = GameTheme.createButton("Готов", GameTheme.ACCENT_WARNING);
        bottomPanel.add(readyButton);

        startGameButton = GameTheme.createButtonLarge("Начать игру", GameTheme.ACCENT_SUCCESS);
        startGameButton.setEnabled(false);
        bottomPanel.add(startGameButton);

        exitButton = GameTheme.createButton("Выход", GameTheme.ACCENT_DANGER);
        bottomPanel.add(exitButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void updatePlayersWithReadyStatus(String[] players, boolean[] readyStatus) {
        playersPanel.removeAll();
        playerCards.clear();

        if (players == null) return;

        for (int i = 0; i < players.length; i++) {
            String playerName = players[i];
            boolean isReady = readyStatus != null && i < readyStatus.length && readyStatus[i];

            PlayerCard card = new PlayerCard(playerName, 0, i, isReady, false);
            playersPanel.add(card);
            playersPanel.add(Box.createVerticalStrut(GameTheme.PADDING_MEDIUM));

            playerCards.put(i, card);
        }

        playersPanel.add(Box.createVerticalGlue());

        playersPanel.revalidate();
        playersPanel.repaint();
    }

    public void setPlayerCount(int current, int max) {
        playerCountLabel.setText(current + "/" + max + " игроков подключено");
    }

    public void setStatus(String status, boolean allReady) {
        statusLabel.setText(status);
        if (allReady) {
            statusLabel.setForeground(GameTheme.ACCENT_SUCCESS);
            startGameButton.setEnabled(true);
        } else {
            statusLabel.setForeground(GameTheme.ACCENT_WARNING);
            startGameButton.setEnabled(false);
        }
    }

    public void setReadyButtonStatus(boolean ready) {
        this.isReady = ready;
        if (ready) {
            readyButton.setText("Отменить");
            readyButton.setBackground(GameTheme.ACCENT_SUCCESS);
        } else {
            readyButton.setText("Готов");
            readyButton.setBackground(GameTheme.ACCENT_WARNING);
        }
    }

    public boolean getReadyStatus() {
        return isReady;
    }

    public void setReadyListener(ActionListener listener) {
        readyButton.addActionListener(listener);
    }

    public void setStartGameListener(ActionListener listener) {
        startGameButton.addActionListener(listener);
    }

    public void setExitListener(ActionListener listener) {
        exitButton.addActionListener(listener);
    }
}
