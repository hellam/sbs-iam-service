# RSA Key Pair Generation Guide

This guide explains how to generate RSA public and private key pairs using OpenSSL for secure password encryption in the Shiva Banking IAM service.

## Prerequisites

- OpenSSL installed on your system
- Basic command-line knowledge
- Access to a secure directory for storing keys

## RSA Key Generation

### Step 1: Generate Private Key

Generate a 2048-bit RSA private key using the following command:

```bash
openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:2048
```

**Command Breakdown:**
- `openssl genpkey`: OpenSSL command for generating private keys
- `-algorithm RSA`: Specifies RSA algorithm
- `-out private.pem`: Output file for the private key
- `-pkeyopt rsa_keygen_bits:2048`: Sets key size to 2048 bits (recommended minimum for security)

### Step 2: Generate Public Key

Extract the public key from the private key:

```bash
openssl rsa -pubout -in private.pem -out public.pem
```

**Command Breakdown:**
- `openssl rsa`: OpenSSL command for RSA key operations
- `-pubout`: Output the public key portion
- `-in private.pem`: Input private key file
- `-out public.pem`: Output file for the public key

## File Permissions

After generating the keys, set appropriate permissions to protect the private key:

```bash
# Make private key readable only by owner
chmod 600 private.pem

# Make public key readable by all (but not writable)
chmod 644 public.pem
```

## Verification

Verify that the keys were generated correctly:

```bash
# Check private key
openssl rsa -in private.pem -check

# Check public key
openssl rsa -pubin -in public.pem -text -noout
```

## Key Usage in Shiva Banking IAM

### Public Key
- **Purpose**: Used by client applications to encrypt passwords before transmission
- **Storage**: Can be safely distributed to client applications
- **Security**: Compromise of public key does not compromise security

### Private Key
- **Purpose**: Used by the server to decrypt passwords received from clients
- **Storage**: Must be stored securely on the server, never exposed to clients
- **Security**: Compromise of private key is a critical security incident

## Key Rotation

For enhanced security, consider rotating keys periodically:

1. Generate new key pair
2. Update server configuration with new private key
3. Distribute new public key to clients
4. Remove old keys after grace period

## Security Best Practices

1. **Never share the private key** with anyone or any external system
2. **Store private keys securely** with restricted access
3. **Use strong key sizes** (2048-bit minimum, 4096-bit recommended)
4. **Regularly rotate keys** as part of security maintenance
5. **Monitor key usage** and implement audit logging
6. **Backup keys securely** with encryption

## Troubleshooting

### Common Issues

**"Permission denied" when generating keys:**
- Ensure you have write permissions in the current directory
- Check if files with same names already exist

**"Invalid key size" error:**
- Ensure you're using a supported key size (1024, 2048, 3072, 4096)
- 2048-bit is recommended for current security standards

**Keys not working in application:**
- Verify the key format (PEM format required)
- Check that private and public keys correspond to each other
- Ensure proper file permissions

### Verification Commands

```bash
# Check if private key is valid
openssl rsa -in private.pem -noout -modulus

# Check if public key matches private key
openssl rsa -in private.pem -pubout -out temp.pub
diff temp.pub public.pem && echo "Keys match" || echo "Keys don't match"
rm temp.pub
```

## Integration with Application

After generating keys:

1. Store the private key securely on your server
2. Configure your application to use the private key for decryption
3. Distribute the public key to client applications for encryption
4. Test the encryption/decryption flow thoroughly

## Additional Resources

- [OpenSSL Documentation](https://www.openssl.org/docs/)
- [RSA Algorithm Overview](https://en.wikipedia.org/wiki/RSA_(cryptosystem))
- [Key Management Best Practices](https://tools.ietf.org/html/rfc5280)</content>
<parameter name="filePath">/Users/hellamimbosa/IdeaProjects/shiva-banking/services/iam-service/RSA_KEY_GENERATION.md
