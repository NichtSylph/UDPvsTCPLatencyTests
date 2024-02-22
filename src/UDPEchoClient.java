package latency.simulator;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

/**
 * The UDP Echo Client class for the network latency and throughput measurement
 * application.
 * This class sends messages to a specified server and measures round-trip time
 * (RTT) and throughput.
 *
 * @author Joel Santos
 * @version 1.0
 * @since 02/10/2024
 */
public class UDPEchoClient {
    private DatagramSocket socket; // Socket to communicate with the server
    private InetAddress address; // Server's IP address
    private int port; // Server's port number
    private long key = 123456789L; // Encryption key for messages

    /**
     * Constructs a UDPEchoClient targeting a specific server.
     *
     * @param host The hostname or IP address of the server.
     * @param port The port number of the server.
     * @throws UnknownHostException If the IP address of the host could not be
     *                              determined.
     * @throws SocketException      If the socket could not be opened.
     */
    public UDPEchoClient(String host, int port) throws UnknownHostException, SocketException {
        this.socket = new DatagramSocket();
        this.address = InetAddress.getByName(host);
        this.port = port;
        this.socket.setSoTimeout(10000); // Set a 10-second timeout for responses
    }

    /**
     * Sends a message to the server and measures the round-trip time (RTT).
     *
     * @param msg The message to be sent.
     * @return The RTT in milliseconds.
     * @throws IOException If an I/O error occurs.
     */
    public long sendEcho(String msg) throws IOException {
        byte[] buf = XOREncryptionService.encryptDecrypt(msg.getBytes(), key);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        long startTime = System.nanoTime();
        socket.send(packet);
        packet = new DatagramPacket(buf, buf.length); // Prepare for receiving the echo
        try {
            socket.receive(packet); // Attempt to receive the echo
        } catch (SocketTimeoutException e) {
            System.err.println("No response from server (timeout).");
            return -1;
        }
        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000; // Convert to milliseconds
    }

    /**
     * Measures RTT and throughput for different message sizes and numbers of
     * messages.
     *
     * @throws IOException If an I/O error occurs.
     */
    public void measureRTTAndThroughput() throws IOException {
        int[] messageSizes = { 8, 64, 512 }; // Message sizes for RTT measurement
        int[][] throughputParams = { { 16384, 64 }, { 4096, 256 }, { 1024, 1024 } }; // Parameters for throughput
                                                                                     // measurement

        // Measure RTT for different message sizes
        for (int size : messageSizes) {
            byte[] message = new byte[size];
            long rtt = sendEcho(new String(message));
            System.out.println("RTT for " + size + " bytes: " + rtt + " ms");
        }

        // Measure throughput for different numbers of datagrams
        for (int[] params : throughputParams) {
            int numDatagrams = params[0];
            int datagramSize = params[1];
            long time = measureThroughput(numDatagrams, datagramSize);
            long throughput = (numDatagrams * datagramSize * 8L) / time; // Calculate throughput in bits per second
            System.out.println("Throughput for " + numDatagrams + " datagrams of " + datagramSize + " bytes: "
                    + throughput + " bps");
        }

        // After measurements, send the termination message
        sendTerminationSignal();
    }

    /**
     * Measures throughput by sending a specified number of datagrams of a certain
     * size.
     *
     * @param numDatagrams The number of datagrams to send.
     * @param datagramSize The size of each datagram in bytes.
     * @return The time taken to send all datagrams in milliseconds.
     * @throws IOException If an I/O error occurs.
     */
    private long measureThroughput(int numDatagrams, int datagramSize) throws IOException {
        byte[] message = new byte[datagramSize];
        Arrays.fill(message, (byte) 'x'); // Fill the message array with some data
        long startTime = System.nanoTime();

        for (int i = 0; i < numDatagrams; i++) {
            byte[] encryptedMessage = XOREncryptionService.encryptDecrypt(message, key);
            DatagramPacket packet = new DatagramPacket(encryptedMessage, encryptedMessage.length, address, port);
            socket.send(packet);
            key = XOREncryptionService.xorShift(key); // Update key after use
        }

        // No acknowledgment from server, so just calculate time
        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000; // Return time in milliseconds
    }

    /**
     * Sends a termination signal to the server to indicate the end of the test.
     *
     * @throws IOException If an I/O error occurs.
     */
    private void sendTerminationSignal() throws IOException {
        byte[] message = "END_OF_MESSAGES".getBytes();
        DatagramPacket packet = new DatagramPacket(message, message.length, address, port);
        socket.send(packet);
    }

    /**
     * Closes the socket and releases any associated resources.
     */
    public void close() {
        socket.close();
    }

    /**
     * Main method to run the UDP Echo Client.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        String host = "pi.cs.oswego.edu";
        int port = 26881;
        try {
            UDPEchoClient client = new UDPEchoClient(host, port);
            client.measureRTTAndThroughput();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
