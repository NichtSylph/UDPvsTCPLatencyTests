package latency.simulator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

/**
 * Implements a UDP Echo Server that listens for incoming UDP packets from
 * clients,
 * decrypts them using XOR encryption, checks for a termination message, and
 * echoes back
 * the decrypted messages after re-encrypting them. The server uses a simple XOR
 * shift-based
 * encryption mechanism provided by the {@link XOREncryptionService}.
 * 
 * @author Joel Santos
 * @version 1.0
 * @since 02/10/2024
 */
public class UDPEchoServer {
    private DatagramSocket socket; // The socket to listen for incoming packets
    private boolean running; // Flag to control the server's running state
    private long key = 123456789L; // Initial key for XOR encryption
    private final byte[] TERMINATION_MESSAGE = "END_OF_MESSAGES".getBytes(); // Predefined termination message

    /**
     * Constructs a new UDPEchoServer bound to the specified port.
     * 
     * @param port The port number on which the server will listen for incoming
     *             packets.
     * @throws IOException If an I/O error occurs.
     */
    public UDPEchoServer(int port) throws IOException {
        socket = new DatagramSocket(port);
        System.out.println("UDP Server is running...");
    }

    /**
     * Starts the server loop, listening for incoming packets and processing them
     * accordingly.
     */
    public void run() {
        running = true;
        while (running) {
            try {
                byte[] buffer = new byte[1024]; // Buffer for incoming packets
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Receive an incoming packet

                byte[] receivedData = Arrays.copyOf(packet.getData(), packet.getLength()); // Extract the actual data
                byte[] decryptedData = XOREncryptionService.encryptDecrypt(receivedData, key); // Decrypt the data
                key = XOREncryptionService.xorShift(key); // Update the encryption key

                // Check if the received message is the termination signal
                if (Arrays.equals(receivedData, TERMINATION_MESSAGE)) {
                    System.out.println("Termination Signal received from client.");
                    running = false; // Stop the server loop
                    break;
                }

                // Encrypt the data again for echoing back
                byte[] processedData = XOREncryptionService.encryptDecrypt(decryptedData, key);
                key = XOREncryptionService.xorShift(key); // Update the key again
                DatagramPacket echoPacket = new DatagramPacket(processedData, processedData.length, packet.getAddress(),
                        packet.getPort());
                socket.send(echoPacket); // Send the echo packet back to the client
            } catch (IOException e) {
                e.printStackTrace();
                running = false; // Stop the server on error
            }
        }
        stopServer(); // Clean up resources
    }

    /**
     * Stops the server and releases the socket.
     */
    private void stopServer() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            System.out.println("Server stopped.");
        }
    }

    /**
     * The main method to start the UDPEchoServer.
     * 
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        int port = 26881; // Default port number
        try {
            UDPEchoServer server = new UDPEchoServer(port);
            server.run(); // Start the server
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
        }
    }
}
