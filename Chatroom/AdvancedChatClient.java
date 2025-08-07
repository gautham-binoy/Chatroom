import java.io.*;
import java.net.*;
import java.util.Scanner;

public class AdvancedChatClient {
    public static void main(String[] args) {
        final String server = "172.18.17.65";
        final int port = 5000;

        try (Socket socket = new Socket(server, port)) {
            System.out.println("Connected to server.");
            new ReadThread(socket).start();

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner sc = new Scanner(System.in);

            while (true) {
                String input = sc.nextLine();

                if (input.startsWith("/code")) {
                    out.println(input); // send /code or /code username
                    System.out.println("Enter code (type /end to finish):");
                    while (true) {
                        String codeLine = sc.nextLine();
                        out.println(codeLine);
                        if (codeLine.equalsIgnoreCase("/end")) break;
                    }
                } else {
                    out.println(input);
                    if (input.equalsIgnoreCase("bye")) break;
                }
            }

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    static class ReadThread extends Thread {
        private BufferedReader in;

        public ReadThread(Socket socket) {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                System.out.println("Error creating input stream.");
            }
        }

        public void run() {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    System.out.println(response);
                }
            } catch (IOException e) {
                System.out.println("Disconnected from server.");
            }
        }
    }
}

