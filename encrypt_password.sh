#!/bin/bash

# Simple password encryption using Java (guaranteed to work)

echo "╔══════════════════════════════════════════════════════════╗"
echo "║          Password Encryption (Using Java)               ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# Check if we're in the right directory
if [ ! -d "services/iam-service" ]; then
    echo "❌ Error: Must run from project root directory"
    echo "Current directory: $(pwd)"
    exit 1
fi

# Get public key from server
echo "Step 1: Getting public key from server..."
PUBLIC_KEY=$(curl -s -X POST http://localhost:9001/api/v1/oauth/identifier/backoffice \
  -H 'Content-Type: application/json' \
  -d '{"identifier":"admin","channel":"INTERNET_BANKING"}' \
  | grep -o '"publicKey":"[^"]*"' | cut -d'"' -f4)

if [ -z "$PUBLIC_KEY" ]; then
    echo "❌ Failed to get public key. Is the server running on port 9001?"
    echo ""
    echo "Start it with:"
    echo "  cd services/iam-service && mvn spring-boot:run"
    exit 1
fi

echo "✅ Got public key from server"
echo ""

# Get password
read -sp "Enter password to encrypt: " PASSWORD
echo ""

if [ -z "$PASSWORD" ]; then
    echo "❌ Password cannot be empty"
    exit 1
fi

# Use Java to encrypt
echo ""
echo "⏳ Encrypting with Java (guaranteed to work)..."

cd services/iam-service

# Create temp Java file
cat > /tmp/QuickEncrypt.java << EOF
import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class QuickEncrypt {
    public static void main(String[] args) throws Exception {
        String publicKeyBase64 = args[0];
        String password = args[1];

        String clean = publicKeyBase64.replaceAll("\\\\s", "");
        byte[] publicKeyBytes = Base64.getDecoder().decode(clean);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        System.out.println(Base64.getEncoder().encodeToString(encrypted));
    }
}
EOF

# Compile and run
javac /tmp/QuickEncrypt.java 2>/dev/null
if [ $? -ne 0 ]; then
    echo "❌ Java compilation failed"
    rm /tmp/QuickEncrypt.java 2>/dev/null
    exit 1
fi

ENCRYPTED=$(java -cp /tmp QuickEncrypt "$PUBLIC_KEY" "$PASSWORD" 2>/dev/null)

# Cleanup
rm /tmp/QuickEncrypt.java /tmp/QuickEncrypt.class 2>/dev/null

cd - > /dev/null

if [ -z "$ENCRYPTED" ]; then
    echo "❌ Encryption failed"
    exit 1
fi

echo "✅ Encryption successful!"
echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║            Encrypted Password (Copy Below)              ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""
echo "$ENCRYPTED"
echo ""
echo "───────────────────────────────────────────────────────────"
echo "📋 Test in Swagger:"
echo "{"
echo "  \"identifier\": \"test@example.com\","
echo "  \"password\": \"$ENCRYPTED\""
echo "}"
echo "───────────────────────────────────────────────────────────"
echo ""

# Test it works
read -p "Test decryption with server? (y/n): " TEST

if [ "$TEST" = "y" ] || [ "$TEST" = "Y" ]; then
    echo ""
    echo "Testing decryption..."
    RESPONSE=$(curl -s -X POST http://localhost:9001/api/v1/oauth/password \
      -H 'Content-Type: application/json' \
      -d "{\"identifier\":\"admin\",\"password\":\"$ENCRYPTED\"}")

    if echo "$RESPONSE" | grep -q "Padding error"; then
        echo "❌ DECRYPTION FAILED! Something is still wrong."
        echo "Response: $RESPONSE"
    elif echo "$RESPONSE" | grep -q "Invalid credentials\|User not found"; then
        echo "✅ DECRYPTION WORKS! (User not found is expected)"
        echo "The encrypted password was successfully decrypted by the server."
    else
        echo "Response: $RESPONSE"
    fi
fi

echo ""

