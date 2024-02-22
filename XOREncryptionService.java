package latency.simulator;

/**
 * Provides XOR encryption and decryption services for messages using a simple
 * XOR shift-based key.
 * This class includes methods for performing the XOR shift operation on a key
 * and for encrypting or decrypting
 * byte arrays (messages) with a given key.
 * 
 * The encryption and decryption process is identical due to the nature of XOR
 * operation, making this
 * service suitable for lightweight, symmetric encryption tasks in the
 * application.
 * 
 * @author Joel Santos
 * @version 1.0
 * @since 02/10/2024
 */
public class XOREncryptionService {

    /**
     * Performs a series of XOR shifts on a given key to generate a new key.
     * This method is used to modify the encryption/decryption key in a predictable
     * manner.
     * 
     * @param r The original key to be shifted.
     * @return The shifted key.
     */
    public static long xorShift(long r) {
        r ^= r << 13;
        r ^= r >>> 7;
        r ^= r << 17;
        return r;
    }

    /**
     * Encrypts or decrypts a given message using XOR operation with a provided key.
     * Due to the symmetric nature of XOR, this method can be used for both
     * encryption and decryption.
     * 
     * @param message The byte array representing the message to be encrypted or
     *                decrypted.
     * @param key     The encryption key used for the XOR operation. Only the least
     *                significant byte is used.
     * @return The resulting byte array after applying the XOR operation with the
     *         key.
     */
    public static byte[] encryptDecrypt(byte[] message, long key) {
        byte[] result = new byte[message.length];
        byte keyByte = (byte) (key & 0xFF); // Use only the least significant byte of the key
        for (int i = 0; i < message.length; i++) {
            result[i] = (byte) (message[i] ^ keyByte); // XOR each byte of the message with the key byte
        }
        return result;
    }
}
