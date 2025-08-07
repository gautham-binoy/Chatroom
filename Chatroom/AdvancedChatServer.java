import java.io.*;
import java.net.*;
import java.util.*;

public class AdvancedChatServer {
    private static final int PORT = 5000;
    private static Map<String, ClientHandler> clients = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        System.out.println("Advanced Code-Sharing Chat Server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private String name;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Ask for unique username
                while (true) {
                    out.println("Enter your username: ");
                    name = in.readLine();
                    if (name == null) return;

                    synchronized (clients) {
                        if (!clients.containsKey(name)) {
                            clients.put(name, this);
                            break;
                        } else {
                            out.println("Username already taken. Try another.");
                        }
                    }
                }

                out.println("Welcome " + name + "! Use /code [user] to send code. Type /end to finish code block.");
                broadcast("Server", name + " has joined the chat.");
                System.out.println(name + " connected.");

                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.equalsIgnoreCase("bye")) break;

                    if (msg.startsWith("/to ")) {
                        String[] parts = msg.split(" ", 3);
                        if (parts.length >= 3) sendPrivate(parts[1], parts[2]);
                        else out.println("Invalid format. Use: /to username message");

                    } else if (msg.startsWith("/code")) {
                        String[] parts = msg.split(" ", 2);
                        String recipient = parts.length == 2 ? parts[1] : null;

                        StringBuilder codeBlock = new StringBuilder();
                        codeBlock.append("[Code from ").append(name).append("]").append("\n");
                        String line;
                        while ((line = in.readLine()) != null && !line.equalsIgnoreCase("/end")) {
                            codeBlock.append(line).append("\n");
                        }
                        String codeMessage = codeBlock.toString();

                        if (recipient != null) {
                            sendPrivateCode(recipient, codeMessage);
                        } else {
                            broadcast("[Code from " + name + "]", codeMessage);
                        }

                    } else {
                        broadcast(name, msg);
                    }
                }

            } catch (IOException e) {
                System.out.println("Error with client " + name);
            } finally {
                try {
                    if (name != null) {
                        clients.remove(name);
                        broadcast("Server", name + " has left the chat.");
                        System.out.println(name + " disconnected.");
                    }
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket for " + name);
                }
            }
        }

        private void sendPrivate(String to, String message) {
            ClientHandler client = clients.get(to);
            if (client != null) {
                client.out.println("[Private] " + name + ": " + message);
                out.println("[To " + to + "] " + message);
            } else {
                out.println("User '" + to + "' not found.");
            }
        }

        private void sendPrivateCode(String to, String code) {
            ClientHandler client = clients.get(to);
            if (client != null) {
                client.out.println("[Private Code from " + name + "]:\n" + code);
                out.println("[To " + to + "]:\n" + code);
                System.out.println("Private code from " + name + " to " + to);
            } else {
                out.println("User '" + to + "' not found.");
            }
        }

        private void broadcast(String sender, String message) {
            String formatted = sender + ": " + message;
            System.out.println("Broadcasting: " + formatted);
            synchronized (clients) {
                for (ClientHandler client : clients.values()) {
                    client.out.println(formatted);
                }
            }
        }
    }
}

