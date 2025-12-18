package ru.kpfu.itis.jackal.ui;

import ru.kpfu.itis.jackal.ui.theme.GameTheme;
import javax.swing.*;

public class AppFrame extends JFrame {

    private JPanel contentPanel;

    public AppFrame(String title, int width, int height) {
        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(width, height);
        setLocationRelativeTo(null);
        setResizable(true);

        contentPanel = new JPanel();
        GameTheme.applyDarkTheme(contentPanel);
        contentPanel.setLayout(new java.awt.BorderLayout());

        setContentPane(contentPanel);
        setVisible(true);
    }

    public void setContent(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, java.awt.BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
}