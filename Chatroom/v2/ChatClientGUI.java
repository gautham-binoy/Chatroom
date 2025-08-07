// --- ChatClientGUI.java ---
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClientGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JTextArea codeArea;
    private JButton sendBtn, sendCodeBtn;
    private JComboBox<String> userList;
    private DefaultComboBoxModel<String> userModel;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ChatClientGUI() {
        showConnectionDialog();
    }

    private void showConnectionDialog() {
        JTextField ipField = new JTextField("localhost");
        JTextField portField = new JTextField("5000");
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Server IP:"));
        panel.add(ipField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Connect to Server", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String ip = ipField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            connectToServer(ip, port);
        }
    }

    private void connectToServer(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(() -> listen()).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to connect: " + e.getMessage());
        }
    }

    private void askUsername() {
        while (true) {
            username = JOptionPane.showInputDialog(frame, "Enter your username:");
            if (username != null && !username.isBlank()) {
                out.println(username);
                break;
            }
        }
    }

    private void setupGUI() {
        frame = new JFrame("Chat - " + username);
        frame.setSize(700, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        sendBtn = new JButton("Send");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);

        codeArea = new JTextArea(4, 30);
        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeArea.setBorder(BorderFactory.createTitledBorder("Code Snippet"));
        sendCodeBtn = new JButton("Send Code");

        JPanel codePanel = new JPanel(new BorderLayout());
        codePanel.add(codeScroll, BorderLayout.CENTER);
        codePanel.add(sendCodeBtn, BorderLayout.EAST);

        userModel = new DefaultComboBoxModel<>();
        userList = new JComboBox<>(userModel);
        userList.addItem("Public");

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JLabel("Send To:"), BorderLayout.WEST);
        topPanel.add(userList, BorderLayout.CENTER);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(codePanel, BorderLayout.SOUTH);
        frame.add(bottomPanel, BorderLayout.PAGE_END);

        frame.setVisible(true);

        sendBtn.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        sendCodeBtn.addActionListener(e -> sendCode());
    }

    private void sendMessage() {
        String msg = messageField.getText().trim();
        if (msg.isEmpty()) return;

        String target = (String) userList.getSelectedItem();
        if (!"Public".equals(target)) {
            out.println("/to " + target + " " + msg);
        } else {
            out.println(msg);
        }
        messageField.setText("");
    }

    private void sendCode() {
        String code = codeArea.getText().trim();
        if (code.isEmpty()) return;

        String target = (String) userList.getSelectedItem();
        if (!"Public".equals(target)) {
            out.println("/code " + target);
        } else {
            out.println("/code " + username); // send to self for broadcast
        }
        for (String line : code.split("\n")) {
            out.println(line);
        }
        out.println("/end");
        codeArea.setText("");
    }

    private void listen() {
    try {
        String msg;
        boolean guiInitialized = false;

        while ((msg = in.readLine()) != null) {
            if (msg.equals("USERNAME_REQUEST")) {
                askUsername();
            } else if (msg.equals("USERNAME_TAKEN")) {
                JOptionPane.showMessageDialog(null, "Username already taken.");
                askUsername();
            } else {
                if (!guiInitialized) {
                    setupGUI(); // ✅ Safe to setup now
                    guiInitialized = true;
                }

                chatArea.append(msg + "\n");
                updateUserList(msg);
            }
        }
    } catch (IOException e) {
        if (chatArea != null) {
            chatArea.append("Connection closed.\n");
        }
    }
}


    private void updateUserList(String msg) {
        if (msg.startsWith("Server") && msg.contains("joined")) {
            String[] parts = msg.split(" ");
            if (parts.length >= 2) {
                String newUser = parts[1];
                if (!newUser.equals(username) && userModel.getIndexOf(newUser) == -1) {
                    userModel.addElement(newUser);
                }
            }
        } else if (msg.startsWith("Server") && msg.contains("left")) {
            String[] parts = msg.split(" ");
            if (parts.length >= 2) {
                userModel.removeElement(parts[1]);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClientGUI::new);
    }
}

