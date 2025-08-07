/* ChatApp.java - Main Launcher with Host and Join Mode, Modern UI */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatApp::showLauncher);
    }

    private static void showLauncher() {
        JFrame frame = new JFrame("Chat App Launcher");
        frame.setSize(400, 250);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Welcome to WhatsApp-style Chat");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton hostButton = new JButton("Start Private Room (LAN Host)");
        JButton joinButton = new JButton("Join Room (LAN/Internet)");

        hostButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        joinButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        hostButton.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        joinButton.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        hostButton.addActionListener(e -> {
            String port = JOptionPane.showInputDialog(frame, "Enter port to host on:", "5000");
            if (port != null && !port.isEmpty()) {
                int p = Integer.parseInt(port);
                new Thread(() -> new ChatServer(p)).start();
                new ChatUI("localhost", p);  // Client connects to own host
                frame.dispose();
            }
        });

        joinButton.addActionListener(e -> {
            JTextField ipField = new JTextField("localhost");
            JTextField portField = new JTextField("5000");
            JPanel inputPanel = new JPanel(new GridLayout(2, 2));
            inputPanel.add(new JLabel("Server IP:")); inputPanel.add(ipField);
            inputPanel.add(new JLabel("Port:")); inputPanel.add(portField);

            int result = JOptionPane.showConfirmDialog(frame, inputPanel, "Join Chat Room", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String ip = ipField.getText();
                int port = Integer.parseInt(portField.getText());
                new ChatUI(ip, port);
                frame.dispose();
            }
        });

        panel.add(Box.createVerticalStrut(20));
        panel.add(title);
        panel.add(Box.createVerticalStrut(30));
        panel.add(hostButton);
        panel.add(Box.createVerticalStrut(15));
        panel.add(joinButton);

        frame.add(panel);
        frame.setVisible(true);
    }
}
