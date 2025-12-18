package ru.kpfu.itis.jackal.ui.screens;

import ru.kpfu.itis.jackal.ui.theme.GameTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class MainMenuScreen extends JPanel {

    private JTextField playerNameField;
    private JTextField hostField;
    private JTextField portField;
    private JLabel statusLabel;
    private JButton connectButton;
    private JRadioButton hostRadio;
    private JRadioButton clientRadio;
    private JButton exitButton;

    public MainMenuScreen() {
        setLayout(new GridBagLayout());
        GameTheme.applyDarkTheme(this);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(GameTheme.PADDING_MEDIUM, GameTheme.PADDING_MEDIUM,
                GameTheme.PADDING_MEDIUM, GameTheme.PADDING_MEDIUM);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel titleLabel = GameTheme.createAccentLabel("ШАКАЛ - ПИРАТСКАЯ ИГРА", GameTheme.FONT_TITLE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);

        JLabel subtitleLabel = GameTheme.createLabel("Подключитесь к игре", GameTheme.FONT_HEADING_3, GameTheme.TEXT_SECONDARY);
        gbc.gridy = 1;
        add(subtitleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;

        JLabel nameLabel = GameTheme.createLabel("Ваше имя:", GameTheme.FONT_BODY, GameTheme.TEXT_PRIMARY);
        add(nameLabel, gbc);

        playerNameField = createStyledTextField("Яромир");
        gbc.gridx = 1;
        add(playerNameField, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel hostLabel = GameTheme.createLabel("Адрес сервера:", GameTheme.FONT_BODY, GameTheme.TEXT_PRIMARY);
        add(hostLabel, gbc);

        hostField = createStyledTextField("localhost");
        gbc.gridx = 1;
        add(hostField, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        JLabel portLabel = GameTheme.createLabel("Порт сервера:", GameTheme.FONT_BODY, GameTheme.TEXT_PRIMARY);
        add(portLabel, gbc);

        portField = createStyledTextField("8888");
        gbc.gridx = 1;
        add(portField, gbc);

        gbc.gridy = 5;
        gbc.gridx = 0;
        JLabel modeLabel = GameTheme.createLabel("Режим подключения:", GameTheme.FONT_BODY, GameTheme.TEXT_PRIMARY);
        add(modeLabel, gbc);

        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, GameTheme.PADDING_MEDIUM, 0));
        modePanel.setBackground(GameTheme.BACKGROUND_PRIMARY);

        hostRadio = new JRadioButton("Я хост (создам игру)");
        hostRadio.setFont(GameTheme.FONT_BODY);
        hostRadio.setBackground(GameTheme.BACKGROUND_PRIMARY);
        hostRadio.setForeground(GameTheme.TEXT_PRIMARY);
        hostRadio.setSelected(true);

        clientRadio = new JRadioButton("Подключиться к игре");
        clientRadio.setFont(GameTheme.FONT_BODY);
        clientRadio.setBackground(GameTheme.BACKGROUND_PRIMARY);
        clientRadio.setForeground(GameTheme.TEXT_PRIMARY);

        ButtonGroup group = new ButtonGroup();
        group.add(hostRadio);
        group.add(clientRadio);

        modePanel.add(hostRadio);
        modePanel.add(clientRadio);

        gbc.gridx = 1;
        add(modePanel, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, GameTheme.PADDING_XLARGE, 0));
        buttonPanel.setBackground(GameTheme.BACKGROUND_PRIMARY);

        connectButton = GameTheme.createButtonLarge("Подключиться к игре", GameTheme.ACCENT_SUCCESS);
        buttonPanel.add(connectButton);

        exitButton = GameTheme.createButton("Выход", GameTheme.ACCENT_DANGER);
        buttonPanel.add(exitButton);

        gbc.gridy = 6;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        statusLabel = GameTheme.createLabel("", GameTheme.FONT_SMALL, GameTheme.ACCENT_DANGER);
        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        add(statusLabel, gbc);
    }

    private JTextField createStyledTextField(String defaultText) {
        JTextField field = new JTextField(defaultText);
        field.setFont(GameTheme.FONT_BODY);
        field.setBackground(GameTheme.BACKGROUND_TERTIARY);
        field.setForeground(GameTheme.TEXT_PRIMARY);
        field.setCaretColor(GameTheme.ACCENT_PRIMARY);
        field.setBorder(BorderFactory.createLineBorder(GameTheme.BORDER_LIGHT, 1));
        field.setPreferredSize(new Dimension(200, 30));
        return field;
    }

    public boolean isHostSelected() {
        return hostRadio.isSelected();
    }

    public String getPlayerName() {
        return playerNameField.getText().trim();
    }

    public String getHost() {
        return hostField.getText().trim();
    }

    public int getPort() {
        try {
            return Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public void setStatus(String status, boolean isError) {
        statusLabel.setText(status);
        if (isError) {
            statusLabel.setForeground(GameTheme.ACCENT_DANGER);
        } else {
            statusLabel.setForeground(GameTheme.ACCENT_SUCCESS);
        }
    }

    public void enableConnect(boolean enabled) {
        connectButton.setEnabled(enabled);
        playerNameField.setEnabled(enabled);
        hostField.setEnabled(enabled);
        portField.setEnabled(enabled);
        hostRadio.setEnabled(enabled);
        clientRadio.setEnabled(enabled);
    }

    public void setConnectListener(ActionListener listener) {
        connectButton.addActionListener(listener);
    }

    public void setExitListener(ActionListener listener) {
        exitButton.addActionListener(listener);
    }
}