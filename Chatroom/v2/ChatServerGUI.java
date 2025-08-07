import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServerGUI {
    private JFrame frame;
    private JTextArea logArea;
    private ServerSocket serverSocket;
    private Map<String, ClientHandler> clients = Collections.synchronizedMap(new HashMap<>());

    public ChatServerGUI(int port) {
        setupGUI();
        startServer(port);
    }

    private void setupGUI() {
        frame = new JFrame("Chat Server");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);

        frame.add(scroll);
        frame.setVisible(true);
    }

    private void startServer(int port) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("Server started on port " + port);

                while (true) {
                    Socket socket = serverSocket.accept();
                    new ClientHandler(socket).start();
                }
            } catch (IOException e) {
                log("Server error: " + e.getMessage());
            }
        }).start();
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    private void broadcast(String sender, String message) {
        log(sender + ": " + message);
        synchronized (clients) {
            for (ClientHandler client : clients.values()) {
                client.send(sender + ": " + message);
            }
        }
    }

    private void privateMessage(String from, String to, String message) {
        ClientHandler target = clients.get(to);
        if (target != null) {
            target.send("[Private] " + from + ": " + message);
            clients.get(from).send("[To " + to + "]: " + message);
            log("[Private] " + from + " -> " + to + ": " + message);
        } else {
            clients.get(from).send("User " + to + " not found.");
        }
    }

    class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void send(String msg) {
            out.println(msg);
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("USERNAME_REQUEST");
                username = in.readLine();

                if (username == null || clients.containsKey(username)) {
                    out.println("USERNAME_TAKEN");
                    socket.close();
                    return;
                }

                clients.put(username, this);
                broadcast("Server", username + " joined.");

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("/to ")) {
                        String[] parts = line.split(" ", 3);
                        if (parts.length == 3) {
                            privateMessage(username, parts[1], parts[2]);
                        }
                    } else {
                        broadcast(username, line);
                    }
                }
            } catch (IOException e) {
                log("Connection lost with " + username);
            } finally {
                try {
                    if (username != null) {
                        clients.remove(username);
                        broadcast("Server", username + " left.");
                    }
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatServerGUI(5000));
    }
}

