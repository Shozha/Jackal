package ru.kpfu.itis.jackal.ui.screens;

import javax.swing.*;
import java.awt.*;

/**
 * MainMenuScreen - главное меню для подключения к серверу
 * Использует Swing
 */
public class MainMenuScreen extends JPanel {
    
    private JTextField playerNameField;
    private JTextField hostField;
    private JTextField portField;
    private JLabel statusLabel;
    private JButton connectButton;
    private JButton exitButton;
    
    private Runnable onConnect;
    
    public MainMenuScreen() {
        setLayout(new GridBagLayout());
        setBackground(new Color(245, 245, 245));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Заголовок
        JLabel titleLabel = new JLabel("Шакал - Pirates Game");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);
        
        JLabel subtitleLabel = new JLabel("Подключитесь к игре");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridy = 1;
        add(subtitleLabel, gbc);
        
        // Имя игрока
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        add(new JLabel("Ваше имя:"), gbc);
        
        playerNameField = new JTextField("Яромир");
        gbc.gridx = 1;
        add(playerNameField, gbc);
        
        // Хост
        gbc.gridy = 3;
        gbc.gridx = 0;
        add(new JLabel("Адрес сервера:"), gbc);
        
        hostField = new JTextField("localhost");
        gbc.gridx = 1;
        add(hostField, gbc);
        
        // Порт
        gbc.gridy = 4;
        gbc.gridx = 0;
        add(new JLabel("Порт сервера:"), gbc);
        
        portField = new JTextField("8888");
        gbc.gridx = 1;
        add(portField, gbc);
        
        // Пробел
        gbc.gridy = 5;
        gbc.gridx = 0;
        add(new JLabel(""), gbc);
        
        // Кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        
        connectButton = new JButton("Подключиться к игре");
        connectButton.setFont(new Font("Arial", Font.BOLD, 14));
        connectButton.setBackground(new Color(76, 175, 80));
        connectButton.setForeground(Color.WHITE);
        connectButton.setFocusPainted(false);
        connectButton.setPreferredSize(new Dimension(200, 40));
        buttonPanel.add(connectButton);
        
        exitButton = new JButton("Выход");
        exitButton.setFont(new Font("Arial", Font.BOLD, 14));
        exitButton.setBackground(new Color(244, 67, 54));
        exitButton.setForeground(Color.WHITE);
        exitButton.setFocusPainted(false);
        exitButton.setPreferredSize(new Dimension(150, 40));
        buttonPanel.add(exitButton);
        
        gbc.gridy = 6;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);
        
        // Статус
        statusLabel = new JLabel("");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(244, 67, 54));
        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        add(statusLabel, gbc);
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
    
    public void setConnectListener(java.awt.event.ActionListener listener) {
        connectButton.addActionListener(listener);
    }
    
    public void setExitListener(java.awt.event.ActionListener listener) {
        exitButton.addActionListener(listener);
    }
    
    public void setStatus(String status, boolean isError) {
        statusLabel.setText(status);
        if (isError) {
            statusLabel.setForeground(new Color(244, 67, 54));
        } else {
            statusLabel.setForeground(new Color(76, 175, 80));
        }
    }
    
    public void enableConnect(boolean enabled) {
        connectButton.setEnabled(enabled);
    }
}