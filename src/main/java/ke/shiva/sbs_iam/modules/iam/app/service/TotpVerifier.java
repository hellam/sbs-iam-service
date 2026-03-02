package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeAuthRepository;
import ke.shiva.shivacorestarter.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class TotpVerifier {

    private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int[] BASE32_LOOKUP = buildBase32Lookup();

    private final CustomerAuthRepository customerAuthRepository;
    private final EmployeeAuthRepository employeeAuthRepository;
    private final PolicyService policyService;
    private final EncryptionUtil encryptionUtil;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${shiva.security.totp.time-step-seconds:30}")
    private int timeStepSeconds;

    @Value("${shiva.security.totp.allowed-past-windows:1}")
    private int allowedPastWindows;

    @Value("${shiva.security.totp.allowed-future-windows:0}")
    private int allowedFutureWindows;

    @Value("${shiva.security.totp.secret-bytes:20}")
    private int secretBytes;

    public boolean verify(IamUserEntity user, String code, Channel channel) {
        if (user == null || !StringUtils.hasText(code)) {
            return false;
        }

        String secret = resolveUserSecret(user);
        if (!StringUtils.hasText(secret)) {
            return false;
        }

        return verifySecret(secret, code, channel);
    }

    public boolean verify(IamUserEntity user, String code) {
        return verify(user, code, Channel.INTERNET_BANKING);
    }

    public boolean verifySecret(String base32Secret, String code, Channel channel) {
        long epochSeconds = System.currentTimeMillis() / 1000;
        return verifySecretAtEpochSeconds(base32Secret, code, channel, epochSeconds);
    }

    /**
     * Test-friendly verification helper that accepts a fixed epoch second.
     */
    boolean verifySecretAtEpochSeconds(String base32Secret, String code, Channel channel, long epochSeconds) {
        if (!StringUtils.hasText(base32Secret) || !StringUtils.hasText(code)) {
            return false;
        }

        int digits = resolveCodeDigits(channel);
        String normalizedCode = code.trim();
        if (!normalizedCode.matches("\\d{" + digits + "}")) {
            return false;
        }

        byte[] secret;
        try {
            secret = decodeBase32(base32Secret);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid TOTP secret format provided for verification.");
            return false;
        }

        long timeCounter = Math.floorDiv(epochSeconds, Math.max(timeStepSeconds, 1));
        long startOffset = -Math.max(allowedPastWindows, 0);
        long endOffset = Math.max(allowedFutureWindows, 0);

        // Grace window support to absorb short client/server clock drift.
        for (long offset = startOffset; offset <= endOffset; offset++) {
            long candidateCounter = timeCounter + offset;
            if (candidateCounter < 0) {
                continue;
            }
            String expected = generateCodeForCounter(secret, candidateCounter, digits);
            if (constantTimeEquals(expected, normalizedCode)) {
                return true;
            }
        }
        return false;
    }

    public String generateSecret() {
        int length = Math.max(secretBytes, 20);
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        return encodeBase32(randomBytes);
    }

    public String buildOtpAuthUri(String base32Secret, String issuer, String accountLabel, int digits, int periodSeconds) {
        String safeIssuer = StringUtils.hasText(issuer) ? issuer.trim() : "Shiva Banking";
        String safeAccountLabel = StringUtils.hasText(accountLabel) ? accountLabel.trim() : "user";
        String encodedLabel = urlEncode(safeIssuer + ":" + safeAccountLabel);
        String encodedIssuer = urlEncode(safeIssuer);
        return "otpauth://totp/" + encodedLabel
                + "?secret=" + base32Secret
                + "&issuer=" + encodedIssuer
                + "&digits=" + digits
                + "&period=" + periodSeconds;
    }

    public int resolveCodeDigits(Channel channel) {
        MfaPolicyEntity mfaPolicy = channel == null ? null : policyService.getMfaPolicy(channel);
        int configured = mfaPolicy != null && mfaPolicy.getOtpLength() != null ? mfaPolicy.getOtpLength() : 6;
        if (configured < 4) {
            return 4;
        }
        return Math.min(configured, 8);
    }

    public int getTimeStepSeconds() {
        return Math.max(timeStepSeconds, 1);
    }

    String generateCodeForEpochSeconds(String base32Secret, long epochSeconds, int digits) {
        byte[] secret = decodeBase32(base32Secret);
        long timeCounter = Math.floorDiv(epochSeconds, Math.max(timeStepSeconds, 1));
        return generateCodeForCounter(secret, timeCounter, digits);
    }

    private String resolveUserSecret(IamUserEntity user) {
        CustomerAuthEntity customerAuth = customerAuthRepository.findByIamUser(user);
        if (customerAuth != null && Boolean.TRUE.equals(customerAuth.getMfaEnabled()) && StringUtils.hasText(customerAuth.getMfaSecret())) {
            return decryptIfEncrypted(customerAuth.getMfaSecret());
        }

        EmployeeAuthEntity employeeAuth = employeeAuthRepository.findByIamUser(user);
        if (employeeAuth != null && Boolean.TRUE.equals(employeeAuth.getMfaEnabled()) && StringUtils.hasText(employeeAuth.getMfaSecret())) {
            return decryptIfEncrypted(employeeAuth.getMfaSecret());
        }

        return null;
    }

    private String decryptIfEncrypted(String storedSecret) {
        if (!StringUtils.hasText(storedSecret)) {
            return null;
        }
        try {
            if (storedSecret.startsWith(EncryptionUtil.PREFIX)) {
                return encryptionUtil.decrypt(storedSecret);
            }
            // Backward compatibility for any legacy plain secrets.
            return storedSecret;
        } catch (Exception ex) {
            log.warn("Unable to decrypt stored TOTP secret.");
            return null;
        }
    }

    private String generateCodeForCounter(byte[] secret, long counter, int digits) {
        byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();
        byte[] hmac = hmacSha1(secret, counterBytes);

        int offset = hmac[hmac.length - 1] & 0x0F;
        int binary = ((hmac[offset] & 0x7F) << 24)
                | ((hmac[offset + 1] & 0xFF) << 16)
                | ((hmac[offset + 2] & 0xFF) << 8)
                | (hmac[offset + 3] & 0xFF);

        int modulo = 1;
        for (int i = 0; i < digits; i++) {
            modulo *= 10;
        }
        int otp = binary % modulo;
        return String.format(Locale.ROOT, "%0" + digits + "d", otp);
    }

    private byte[] hmacSha1(byte[] secret, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            return mac.doFinal(data);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate TOTP HMAC", ex);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private static String encodeBase32(byte[] data) {
        StringBuilder output = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = data[0];
        int next = 1;
        int bitsLeft = 8;
        while (bitsLeft > 0 || next < data.length) {
            if (bitsLeft < 5) {
                if (next < data.length) {
                    buffer <<= 8;
                    buffer |= data[next++] & 0xFF;
                    bitsLeft += 8;
                } else {
                    int pad = 5 - bitsLeft;
                    buffer <<= pad;
                    bitsLeft += pad;
                }
            }
            int index = (buffer >> (bitsLeft - 5)) & 0x1F;
            bitsLeft -= 5;
            output.append(BASE32_ALPHABET[index]);
        }
        return output.toString();
    }

    private static byte[] decodeBase32(String base32) {
        String normalized = base32.trim().replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Base32 input is empty");
        }

        int expectedBytes = normalized.length() * 5 / 8;
        byte[] result = new byte[expectedBytes];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c >= BASE32_LOOKUP.length || BASE32_LOOKUP[c] < 0) {
                throw new IllegalArgumentException("Invalid Base32 character");
            }
            buffer <<= 5;
            buffer |= BASE32_LOOKUP[c];
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }

        if (index == result.length) {
            return result;
        }

        byte[] trimmed = new byte[index];
        System.arraycopy(result, 0, trimmed, 0, index);
        return trimmed;
    }

    private static int[] buildBase32Lookup() {
        int[] lookup = new int[128];
        for (int i = 0; i < lookup.length; i++) {
            lookup[i] = -1;
        }
        for (int i = 0; i < BASE32_ALPHABET.length; i++) {
            lookup[BASE32_ALPHABET[i]] = i;
        }
        return lookup;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
