package ke.shiva.sbs_iam.util;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

/**
 * Utility to encrypt passwords for testing APIs on Swagger.
 *
 * Usage:
 * 1. Get the public key from the /identify endpoint
 * 2. Run this class with the public key and password
 * 3. Copy the encrypted output to Swagger
 *
 * Example:
 * <pre>
 * java PasswordEncryptionTester
 *
 * Enter public key (base64): MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
 * Enter password to encrypt: MyPassword123!
 *
 * Encrypted password (copy this to Swagger):
 * ZW5jcnlwdGVkX3Bhc3N3b3JkX2hlcmU=
 * </pre>
 */
public class PasswordEncryptionTester {

    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);

            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║     Password Encryption Utility for Swagger Testing     ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println();

            // Option 1: Quick test with hardcoded values
            System.out.println("Choose an option:");
            System.out.println("1. Quick test (uses default public key from /identify)");
            System.out.println("2. Custom test (enter your own public key)");
            System.out.print("\nEnter choice (1 or 2): ");

            String choice = scanner.nextLine().trim();

            String publicKeyBase64;

            if ("1".equals(choice)) {
                System.out.println("\n📝 First, call POST /api/v1/iam/identify endpoint to get:");
                System.out.println("   - publicKey");
                System.out.println("   - flowId (used as session ID)\n");
                System.out.print("Enter session ID (flowId from /identify response): ");
                String sessionId = scanner.nextLine().trim();
                System.out.print("Enter public key (base64) from /identify response: ");
                publicKeyBase64 = scanner.nextLine().trim();

                if (sessionId.isEmpty()) {
                    System.err.println("❌ Error: Session ID cannot be empty!");
                    System.exit(1);
                }

                System.out.print("\nEnter password to encrypt: ");
                String password = scanner.nextLine();

                if (password.isEmpty()) {
                    System.err.println("❌ Error: Password cannot be empty!");
                    System.exit(1);
                }

                System.out.println("\n⏳ Encrypting password...");

                // Encrypt the password
                String encryptedPassword = encryptPassword(password, publicKeyBase64);

                // Add session ID salt
                String saltedPassword = sessionId + ":" + encryptedPassword;

                System.out.println("\n✅ Encryption successful!");
                System.out.println("\n╔══════════════════════════════════════════════════════════╗");
                System.out.println("║          Salted Encrypted Password (Copy Below)         ║");
                System.out.println("╚══════════════════════════════════════════════════════════╝");
                System.out.println();
                System.out.println(saltedPassword);
                System.out.println();
                System.out.println("Format: sessionId:encryptedPassword");
                System.out.println("───────────────────────────────────────────────────────────");
                System.out.println("📋 Copy the salted password above and paste it into:");
                System.out.println("   - Swagger UI password field");
                System.out.println("   - POST /api/v1/iam/password request body");
                System.out.println("───────────────────────────────────────────────────────────");
                System.out.println();
            } else {
                System.out.print("\nEnter session ID: ");
                String sessionId = scanner.nextLine().trim();
                System.out.print("Enter public key (base64): ");
                publicKeyBase64 = scanner.nextLine().trim();

                if (sessionId.isEmpty()) {
                    System.err.println("❌ Error: Session ID cannot be empty!");
                    System.exit(1);
                }

                if (publicKeyBase64.isEmpty()) {
                    System.err.println("❌ Error: Public key cannot be empty!");
                    System.exit(1);
                }

                System.out.print("\nEnter password to encrypt: ");
                String password = scanner.nextLine();

                if (password.isEmpty()) {
                    System.err.println("❌ Error: Password cannot be empty!");
                    System.exit(1);
                }

                System.out.println("\n⏳ Encrypting password...");

                // Add session ID salt
                String saltedPassword = sessionId + ":" + password;
                // Encrypt the password
                String encryptedPassword = encryptPassword(saltedPassword, publicKeyBase64);


                System.out.println("\n✅ Encryption successful!");
                System.out.println("\n╔══════════════════════════════════════════════════════════╗");
                System.out.println("║          Salted Encrypted Password (Copy Below)         ║");
                System.out.println("╚══════════════════════════════════════════════════════════╝");
                System.out.println();
                System.out.println(encryptedPassword);
                System.out.println();
                System.out.println("Format: sessionId:password");
                System.out.println("───────────────────────────────────────────────────────────");
                System.out.println("📋 Use this in your API requests");
                System.out.println("───────────────────────────────────────────────────────────");
                System.out.println();
            }

            // Ask if they want to encrypt another password
            System.out.print("Encrypt another password? (y/n): ");
            String again = scanner.nextLine().trim();

            if ("y".equalsIgnoreCase(again) || "yes".equalsIgnoreCase(again)) {
                scanner.close();
                main(args); // Restart
            } else {
                System.out.println("\n👋 Goodbye!");
                scanner.close();
            }

        } catch (Exception e) {
            System.err.println("\n❌ Error during encryption: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Encrypts a password using RSA-OAEP with SHA-256.
     *
     * @param password The plaintext password to encrypt
     * @param publicKeyBase64 The base64-encoded public key (from /identify endpoint)
     * @return The base64-encoded encrypted password
     */
    public static String encryptPassword(String password, String publicKeyBase64) throws Exception {
        // Clean the public key (remove PEM headers if present)
        String cleanKey = publicKeyBase64
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        // Decode the base64 public key
        byte[] publicKeyBytes = Base64.getDecoder().decode(cleanKey);

        // Create PublicKey object
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        // Initialize cipher with RSA-OAEP
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        // Encrypt the password
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedBytes = cipher.doFinal(passwordBytes);

        // Return base64-encoded encrypted data
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Quick method to encrypt a password with a known public key and session ID.
     */
    public static String quickEncrypt(String password, String publicKeyBase64, String sessionId) {
        try {
            String encrypted = encryptPassword(password, publicKeyBase64);
            return sessionId + ":" + encrypted;
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }

    /**
     * Quick method to encrypt a password with a known public key (no session salt).
     */
    public static String quickEncrypt(String password, String publicKeyBase64) {
        try {
            return encryptPassword(password, publicKeyBase64);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }

    /**
     * Example usage in test code.
     */
    public static void example() {
        String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."; // From /identify
        String password = "MyPassword123!";

        String encrypted = quickEncrypt(password, publicKey);
        System.out.println("Encrypted: " + encrypted);

        // Use in Swagger:
        // {
        //   "identifier": "user@example.com",
        //   "password": "<paste encrypted value here>"
        // }
    }
}

