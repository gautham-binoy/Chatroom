import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatUI {
    private JFrame frame;
    private JPanel chatPanel;
    private JTextField inputField;
    private JButton sendBtn, codeBtn, fileBtn;
    private JComboBox<String> userListBox;
    private DefaultComboBoxModel<String> userModel;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private Socket socket;

    public ChatUI(String ip, int port) {
        setupGUI();
        connectToServer(ip, port);
    }

    private void setupGUI() {
        frame = new JFrame("Chat Room");
        frame.setSize(700, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(chatPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendBtn = new JButton("Send");
        codeBtn = new JButton("Code");
        fileBtn = new JButton("File");

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(sendBtn);
        btnPanel.add(codeBtn);
        btnPanel.add(fileBtn);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(btnPanel, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        userModel = new DefaultComboBoxModel<>();
        userListBox = new JComboBox<>(userModel);
        frame.add(userListBox, BorderLayout.WEST);

        sendBtn.addActionListener(e -> sendMessage(false));
        codeBtn.addActionListener(e -> sendMessage(true));
        fileBtn.addActionListener(e -> sendFile());

        frame.setVisible(true);
    }

    private void connectToServer(String ip, int port) {
        try {
            username = JOptionPane.showInputDialog(frame, "Enter username:");
            if (username == null || username.trim().isEmpty()) System.exit(0);

            socket = new Socket(ip, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(username);

            new Thread(() -> {
                try {
                    Object obj;
                    while ((obj = in.readObject()) != null) {
                        if (obj instanceof Message msg) handleMessage(msg);
                    }
                } catch (Exception ignored) {}
            }).start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Connection failed: " + e.getMessage());
            System.exit(0);
        }
    }

    private void handleMessage(Message msg) {
        SwingUtilities.invokeLater(() -> {
            switch (msg.getType()) {
                case TEXT, CODE -> addBubble(msg);
                case FILE -> {
                    try {
                        FileTransfer.saveBytesToFile(msg.getFileData(), msg.getFileName());
                        addSystemMessage("File received from " + msg.getFrom() + ": " + msg.getFileName());
                    } catch (IOException e) {
                        addSystemMessage("Failed to save file: " + msg.getFileName());
                    }
                }
                case SYSTEM -> addSystemMessage(msg.getContent());
                case USER_LIST -> {
                    userModel.removeAllElements();
                    userModel.addElement("Public");
                    for (String name : msg.getContent().split(",")) {
                        if (!name.equals(username)) userModel.addElement(name);
                    }
                }
            }
        });
    }

    private void sendMessage(boolean isCode) {
        String content = inputField.getText().trim();
        if (content.isEmpty()) return;
        inputField.setText("");

        String toUser = userListBox.getSelectedItem().toString();
        String to = toUser.equals("Public") ? null : toUser;
        Message.MessageType type = isCode ? Message.MessageType.CODE : Message.MessageType.TEXT;

        Message msg = new Message(type, username, to, content);
        try {
            out.writeObject(msg);
        } catch (IOException e) {
            addSystemMessage("Failed to send message.");
        }
    }

    private void sendFile() {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(frame);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                byte[] data = FileTransfer.fileToBytes(file);
                String toUser = userListBox.getSelectedItem().toString();
                String to = toUser.equals("Public") ? null : toUser;
                Message msg = new Message(username, to, file.getName(), data);
                out.writeObject(msg);
                addSystemMessage("File sent: " + file.getName());
            } catch (IOException e) {
                addSystemMessage("Failed to send file: " + e.getMessage());
            }
        }
    }

    private void addBubble(Message msg) {
        JPanel bubble = new JPanel();
        bubble.setLayout(new BorderLayout());
        bubble.setBackground(msg.getFrom().equals(username) ? new Color(220, 248, 198) : new Color(255, 255, 255));
        bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 10, 5, 10),
                BorderFactory.createLineBorder(Color.GRAY, 1)));

        JTextArea text = new JTextArea(msg.getContent());
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setFont(msg.getType() == Message.MessageType.CODE ? new Font("Monospaced", Font.PLAIN, 12) : new Font("Segoe UI", Font.PLAIN, 14));
        text.setOpaque(false);

        JLabel label = new JLabel(msg.getFrom());
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));

        bubble.add(label, BorderLayout.NORTH);
        bubble.add(text, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new FlowLayout(msg.getFrom().equals(username) ? FlowLayout.RIGHT : FlowLayout.LEFT));
        wrap.add(bubble);
        wrap.setOpaque(false);
        chatPanel.add(wrap);
        chatPanel.revalidate();
        frame.repaint();
    }

    private void addSystemMessage(String msg) {
        JLabel label = new JLabel("[System] " + msg);
        label.setForeground(Color.GRAY);
        chatPanel.add(label);
        chatPanel.revalidate();
    }
}