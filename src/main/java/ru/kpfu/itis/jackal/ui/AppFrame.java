package ru.kpfu.itis.jackal.ui;

import javax.swing.*;

/**
 * AppFrame - главное окно приложения
 */
public class AppFrame extends JFrame {
    
    private JPanel contentPanel;
    
    public AppFrame(String title, int width, int height) {
        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(width, height);
        setLocationRelativeTo(null);
        setResizable(true);
        
        // Контейнер для смены экранов
        contentPanel = new JPanel();
        setContentPane(contentPanel);
        
        setVisible(true);
    }
    
    public void setContent(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
}
