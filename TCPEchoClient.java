package latency.simulator;

import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * The TCP Echo Client class for the network latency and throughput measurement
 * application.
 * This class establishes a TCP connection to a specified server, sends messages
 * of various sizes,
 * measures round-trip time (RTT) and throughput, and then terminates the
 * connection.
 *
 * @author Joel Santos
 * @version 1.0
 * @since 02/10/2024
 */
public class TCPEchoClient {
    private String host; // Server host address
    private int port; // Server port
    private Socket socket; // Socket for TCP connection
    private DataOutputStream out; // Output stream to server
    private DataInputStream in; // Input stream from server
    private long key = 123456789L; // Encryption key for messages

    /**
     * Constructs a TCPEchoClient with specified host and port.
     *
     * @param host The server host address.
     * @param port The server port.
     */
    public TCPEchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Establishes a connection to the server and initializes data streams.
     *
     * @throws IOException If an I/O error occurs when creating the socket or
     *                     streams.
     */
    public void connect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setSoTimeout(10000);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }

    /**
     * Measures RTT and throughput for different message sizes and sends a
     * termination signal.
     *
     * @throws IOException If an I/O error occurs during communication.
     */
    public void measureRTTAndThroughput() throws IOException {
        // Message sizes and throughput parameters for testing
        int[] messageSizes = { 8, 64, 512 };
        int[][] throughputParams = { { 16384, 64 }, { 4096, 256 }, { 1024, 1024 } };

        // Measure RTT for each message size
        for (int size : messageSizes) {
            byte[] message = new byte[size];
            long rtt = measureRTT(message);
            System.out.println("RTT for " + size + " bytes: " + rtt + " ms");
        }

        // Measure throughput for each parameter set
        for (int[] params : throughputParams) {
            int numMessages = params[0];
            int messageSize = params[1];
            long time = measureThroughput(numMessages, messageSize);
            long throughput = (numMessages * messageSize * 8L) / time;
            System.out.println(
                    "Throughput for " + numMessages + " messages of " + messageSize + " bytes: " + throughput + " bps");
        }

        // Send termination signal to the server
        sendTerminationSignal();
    }

    /**
     * Measures the throughput for a specified number of messages and message size.
     *
     * @param numMessages The number of messages to send.
     * @param messageSize The size of each message in bytes.
     * @return The time taken for the throughput test in milliseconds.
     * @throws IOException If an I/O error occurs during communication.
     */
    public long measureThroughput(int numMessages, int messageSize) throws IOException {
        byte[] message = new byte[messageSize];
        long startTime = System.nanoTime();

        // Send encrypted messages to the server
        for (int i = 0; i < numMessages; i++) {
            byte[] encryptedMessage = XOREncryptionService.encryptDecrypt(message, key);
            key = XOREncryptionService.xorShift(key);
            out.writeInt(encryptedMessage.length);
            out.write(encryptedMessage);
        }

        // Wait for all echoed messages before signaling the end of the test
        for (int i = 0; i < numMessages; i++) {
            int length = in.readInt();
            byte[] receivedMessage = new byte[length];
            in.readFully(receivedMessage);
        }

        // Signal the end of throughput test and wait for acknowledgment
        out.writeInt(0);
        out.flush();
        int ack = in.readInt();
        if (ack != 0) {
            throw new IOException("Incorrect acknowledgment received from server.");
        }

        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000; // Time in milliseconds
    }

    /**
     * Measures the round-trip time (RTT) for a single message of specified size.
     *
     * @param message The message to send.
     * @return The RTT in milliseconds.
     * @throws IOException If an I/O error occurs during communication.
     */
    public long measureRTT(byte[] message) throws IOException {
        long startTime = System.nanoTime();
        byte[] encryptedMessage = XOREncryptionService.encryptDecrypt(message, key);
        out.writeInt(encryptedMessage.length);
        out.write(encryptedMessage);

        int length = in.readInt();
        byte[] receivedMessage = new byte[length];
        in.readFully(receivedMessage);
        byte[] decryptedMessage = XOREncryptionService.encryptDecrypt(receivedMessage, key);

        if (!Arrays.equals(message, decryptedMessage)) {
            throw new IOException("Echoed message does not match the original message.");
        }

        key = XOREncryptionService.xorShift(key); // Update key after decryption

        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000; // Time in milliseconds
    }

    /**
     * Sends a termination signal to the server to indicate the end of the test.
     *
     * @throws IOException If an I/O error occurs during communication.
     */
    private void sendTerminationSignal() throws IOException {
        out.writeInt(-1); // Termination signal
        out.flush();
    }

    /**
     * Closes the socket and data streams.
     *
     * @throws IOException If an I/O error occurs when closing the resources.
     */
    public void close() throws IOException {
        if (socket != null) {
            out.close();
            in.close();
            socket.close();
        }
    }

    /**
     * Main method to run the TCP Echo Client.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) throws IOException {
        String host = "pi.cs.oswego.edu";
        int port = 26881;
        TCPEchoClient client = new TCPEchoClient(host, port);
        try {
            client.connect();
            client.measureRTTAndThroughput();
        } finally {
            client.close();
        }
    }
}
