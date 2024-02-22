package latency.simulator;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The TCP Echo Server class for the network latency and throughput measurement
 * application.
 * This class listens for incoming TCP connections on a specified port, echoes
 * received messages,
 * and handles termination signals from clients.
 *
 * @author Joel Santos
 * @version 1.0
 * @since 02/10/2024
 */
public class TCPEchoServer {
    private ServerSocket serverSocket; // Server socket to listen for incoming connections
    private volatile boolean running = true; // Flag to control the server's running state
    private long key = 123456789L; // Encryption key for messages

    /**
     * Constructs a TCPEchoServer to listen on the specified port.
     *
     * @param port The port number on which the server will listen.
     * @throws IOException If an I/O error occurs when opening the server socket.
     */
    public TCPEchoServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("TCP Server is running...");
        // Add a shutdown hook to clean up resources on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    /**
     * Starts the server, accepting client connections and handling them.
     */
    public void start() {
        while (running && !serverSocket.isClosed()) {
            try (Socket clientSocket = serverSocket.accept()) {
                System.out.println("Client connected");
                handleClient(clientSocket);
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error handling client: " + e.getMessage());
                } else {
                    System.out.println("Server is shutting down...");
                }
            }
        }
    }

    /**
     * Handles communication with a connected client.
     *
     * @param clientSocket The socket representing the client connection.
     */
    private void handleClient(Socket clientSocket) {
        try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            while (running) {
                int length = in.readInt();
                if (length == -1) {
                    // Termination signal received, prepare to stop the server
                    System.out.println("Termination signal received from client.");
                    running = false;
                    break;
                } else if (length == 0) {
                    // End of throughput test signal received, send acknowledgment
                    out.writeInt(0);
                    out.flush();
                    continue;
                }

                // Echo back the received message
                byte[] receivedMessage = new byte[length];
                in.readFully(receivedMessage);
                out.writeInt(length);
                out.write(receivedMessage);
                out.flush();

                key = XOREncryptionService.xorShift(key); // Update encryption key
            }
        } catch (IOException e) {
            System.err.println("Client finished or error occurred: " + e.getMessage());
        } finally {
            try {
                clientSocket.close(); // Ensure the client socket is closed
                System.out.println("Client socket closed.");
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    /**
     * Stops the server, closing the server socket and setting the running flag to
     * false.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // Close the server socket
                System.out.println("Server socket closed.");
            }
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
        System.out.println("Server stopped.");
    }

    /**
     * Main method to run the TCP Echo Server.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        int port = 26881; // Specify the port to listen on
        try {
            TCPEchoServer server = new TCPEchoServer(port);
            server.start(); // Start the server
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
        }
    }
}
