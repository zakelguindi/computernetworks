import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* 
 * This class does all of the group chat server's job
 * It has a list of all connected clients
 * It has a thread-safe list to store all client handlers
 * It has a main server thread that listens for client connections
 * It has a client handler thread that handles individual client connections
 * It has a method to broadcast a message to all connected clients
 * It has a method to send a message to this specific client

 */
public class GroupChatServer implements Runnable {
    // Thread-safe list to store all client handlers
    private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private BufferedReader fromUserReader;
    private PrintWriter toSockWriter; 
    private ServerSocket serverSocket;

    // Constructor for the main server thread
    public GroupChatServer(BufferedReader reader, PrintWriter writer) {
        this.fromUserReader = reader;
        this.toSockWriter = writer;
    }

    // Inner class to handle individual client connections
    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String username;

        // Constructor for the client handler thread   
        // It creates a new client handler thread for a new client connection
        public ClientHandler(Socket socket) throws IOException {
            this.clientSocket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        }

        // The client handler thread starts here
        public void run() {
            try {
                // First message from client should be their username
                username = reader.readLine();
                if (username == null) {
                    System.out.println("Client disconnected before sending username");
                    return; // Client disconnected before sending username
                }

                // Broadcast that new user has joined
                broadcastMessage("Server", username + " has joined the chat");

                // Keep reading messages from this client
                while (true) {
                    String message = reader.readLine();
                    if (message == null) {
                        break; // Client disconnected
                    }
                    broadcastMessage(username, message);
                }
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            } 
            clients.remove(this);
            broadcastMessage("Server", username + " has left the chat");
            try {
                clientSocket.close();
            } catch (Exception e) {
                System.out.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    // Crucial to use this instead of "toSockWriter.println()". BroadcastMessage sends to all clients. 
    private void broadcastMessage(String sender, String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.username != null) { // Only send to clients that have set their username
                    client.writer.println(sender + ": " + message);
                    client.writer.flush();
                }
            }
        }
    }

    // Main server thread run method
    public void run() {
        try {
            // Keep reading from server console and broadcasting
            while (true) {
                String message = fromUserReader.readLine();
                if (message == null) {
                    break; // EOF received
                }
                broadcastMessage("Server", message);
            }
        } catch (IOException e) {
            System.out.println("Error in server thread: " + e.getMessage());
        }
    }

    // Main method to start the server
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java GroupChatServer <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        ServerSocket serverSocket = null;

        try {
            // Create server socket
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            // Set up server console input/output
            BufferedReader fromUserReader = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter toSockWriter = new PrintWriter(System.out, true);

            // Create and start server thread for console input
            GroupChatServer server = new GroupChatServer(fromUserReader, toSockWriter);
            Thread serverThread = new Thread(server);
            serverThread.start();

            // Accept client connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from " + 
                    clientSocket.getInetAddress().getHostAddress() + ":" + 
                    clientSocket.getPort());

                // Create and start new client handler thread
                ClientHandler clientHandler = server.new ClientHandler(clientSocket);
                server.clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
