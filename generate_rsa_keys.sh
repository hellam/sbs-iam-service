#!/bin/bash

# RSA Key Generation Script for Shiva Banking Platform
# Generates JWT, Downstream JWT, and SPA RSA keypairs and outputs as environment variables

set -e

echo "=========================================="
echo "  RSA Key Generation for Shiva Banking"
echo "=========================================="
echo ""

# Check if openssl is available
if ! command -v openssl &> /dev/null; then
    echo "❌ Error: openssl is not installed"
    echo "Please install openssl first:"
    echo "  - macOS: brew install openssl"
    echo "  - Ubuntu/Debian: apt-get install openssl"
    echo "  - CentOS/RHEL: yum install openssl"
    exit 1
fi

# Function to generate keypair
generate_keypair() {
    local name=$1
    local bits=${2:-2048}

    # Generate private key
    openssl genrsa -out ${name}_private.pem $bits 2>/dev/null

    # Extract public key
    openssl rsa -in ${name}_private.pem -pubout -out ${name}_public.pem 2>/dev/null

    # Convert private key to PKCS8 DER format and base64 encode
    local private_key
    private_key=$(openssl pkcs8 -topk8 -inform PEM -outform DER -in ${name}_private.pem -nocrypt | base64 | tr -d '\n')

    # Convert public key to DER format and base64 encode
    local public_key
    public_key=$(openssl rsa -in ${name}_private.pem -pubout -outform DER 2>/dev/null | base64 | tr -d '\n')

    # Clean up PEM files
    rm ${name}_private.pem ${name}_public.pem

    echo "$public_key|$private_key"
}

# Generate JWT keys
echo ""
echo "1. Generating JWT Signing Keys..."
echo "   Purpose: Sign and verify IAM-issued client JWT tokens"
echo ""
jwt_keys=$(generate_keypair "jwt" 2048)
JWT_PUBLIC=$(echo $jwt_keys | cut -d'|' -f1)
JWT_PRIVATE=$(echo $jwt_keys | cut -d'|' -f2)
echo "✅ JWT keys generated"

# Generate Downstream JWT keys (Gateway -> Services)
echo ""
echo "2. Generating Downstream JWT Keys..."
echo "   Purpose: Gateway signs downstream JWT tokens that services verify"
echo ""
downstream_jwt_keys=$(generate_keypair "downstream_jwt" 2048)
DOWNSTREAM_JWT_PUBLIC=$(echo $downstream_jwt_keys | cut -d'|' -f1)
DOWNSTREAM_JWT_PRIVATE=$(echo $downstream_jwt_keys | cut -d'|' -f2)
echo "✅ Downstream JWT keys generated"

# Generate SPA keys
echo ""
echo "3. Generating SPA Password Encryption Keys..."
echo "   Purpose: Encrypt/decrypt passwords from frontend"
echo ""
spa_keys=$(generate_keypair "spa" 2048)
SPA_PUBLIC=$(echo $spa_keys | cut -d'|' -f1)
SPA_PRIVATE=$(echo $spa_keys | cut -d'|' -f2)
echo "✅ SPA keys generated"

# Create .env file
echo ""
echo "=========================================="
echo "  Keys Generated Successfully!"
echo "=========================================="
echo ""
echo "Creating .env file..."

cat > .env.keys << EOF
# Generated on $(date)
# RSA Keys for Shiva Banking Platform

# ============================================
# JWT Keys (IAM -> Client, for token signing)
# ============================================
SHIVA_SECURITY_JWT_PUBLIC_KEY=$JWT_PUBLIC
SHIVA_SECURITY_JWT_PRIVATE_KEY=$JWT_PRIVATE

# ============================================
# Downstream JWT Keys (Gateway -> Services)
# ============================================
SHIVA_DOWNSTREAM_JWT_PUBLIC_KEY=$DOWNSTREAM_JWT_PUBLIC
SHIVA_DOWNSTREAM_JWT_PRIVATE_KEY=$DOWNSTREAM_JWT_PRIVATE

# ============================================
# SPA Keys (for password encryption)
# ============================================
SHIVA_SECURITY_SPA_PUBLIC_KEY=$SPA_PUBLIC
SHIVA_SECURITY_SPA_PRIVATE_KEY=$SPA_PRIVATE

# ============================================
# Usage Instructions
# ============================================
# 1. Source this file: source .env.keys
# 2. Or copy to your main .env file
# 3. For Docker: reference in docker-compose.yml
# 4. For K8s: create secrets from these values
EOF

echo "✅ Keys saved to .env.keys"
echo ""

# Display keys
echo "=========================================="
echo "  Environment Variables"
echo "=========================================="
echo ""
echo "Copy these to your environment or .env file:"
echo ""
echo "# JWT Keys (IAM -> Client)"
echo "export SHIVA_SECURITY_JWT_PUBLIC_KEY=\"$JWT_PUBLIC\""
echo "export SHIVA_SECURITY_JWT_PRIVATE_KEY=\"$JWT_PRIVATE\""
echo ""
echo "# Downstream JWT Keys (Gateway -> Services)"
echo "export SHIVA_DOWNSTREAM_JWT_PUBLIC_KEY=\"$DOWNSTREAM_JWT_PUBLIC\""
echo "export SHIVA_DOWNSTREAM_JWT_PRIVATE_KEY=\"$DOWNSTREAM_JWT_PRIVATE\""
echo ""
echo "# SPA Keys"
echo "export SHIVA_SECURITY_SPA_PUBLIC_KEY=\"$SPA_PUBLIC\""
echo "export SHIVA_SECURITY_SPA_PRIVATE_KEY=\"$SPA_PRIVATE\""
echo ""

# Create Kubernetes secret YAML
echo "Creating Kubernetes secret manifest..."
cat > k8s-rsa-keys-secret.yaml << EOF
apiVersion: v1
kind: Secret
metadata:
  name: shiva-rsa-keys
  namespace: default
type: Opaque
data:
  jwt-public-key: $(echo -n "$JWT_PUBLIC" | base64)
  jwt-private-key: $(echo -n "$JWT_PRIVATE" | base64)
  downstream-jwt-public-key: $(echo -n "$DOWNSTREAM_JWT_PUBLIC" | base64)
  downstream-jwt-private-key: $(echo -n "$DOWNSTREAM_JWT_PRIVATE" | base64)
  spa-public-key: $(echo -n "$SPA_PUBLIC" | base64)
  spa-private-key: $(echo -n "$SPA_PRIVATE" | base64)
EOF

echo "✅ Kubernetes secret saved to k8s-rsa-keys-secret.yaml"
echo ""

# Security instructions
echo "=========================================="
echo "  Security Instructions"
echo "=========================================="
echo ""
echo "⚠️  IMPORTANT:"
echo "1. ✅ Store these keys securely (AWS Secrets Manager, Vault, etc.)"
echo "2. ✅ Never commit .env.keys to Git"
echo "3. ✅ Use different keys for dev/staging/production"
echo "4. ✅ Rotate keys periodically (recommended: every 90 days)"
echo "5. ✅ Restrict access to key files"
echo "6. ✅ Delete .env.keys after copying to secure location"
echo ""

# Add to .gitignore if it exists
if [ -f .gitignore ]; then
    if ! grep -q ".env.keys" .gitignore; then
        echo ".env.keys" >> .gitignore
        echo "k8s-rsa-keys-secret.yaml" >> .gitignore
        echo "✅ Added key files to .gitignore"
    fi
fi

echo "=========================================="
echo "  Next Steps"
echo "=========================================="
echo ""
echo "1. Source the keys:"
echo "   source .env.keys"
echo ""
echo "2. Start the application:"
echo "   ./mvnw spring-boot:run"
echo ""
echo "3. Check logs for validation:"
echo "   Look for '✅ JWT keypair validation successful'"
echo "   Look for '✅ SPA keypair validation successful'"
echo ""
echo "4. For Docker:"
echo "   docker-compose --env-file .env.keys up"
echo ""
echo "5. For Kubernetes:"
echo "   kubectl apply -f k8s-rsa-keys-secret.yaml"
echo ""
echo "=========================================="
echo "  ✨ Key Generation Complete!"
echo "=========================================="