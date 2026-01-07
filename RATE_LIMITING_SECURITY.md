# Rate Limiting and API Security Implementation

## Overview

This document describes the comprehensive rate limiting and API security features implemented in the Shiva Banking platform, providing Laravel-equivalent security capabilities for Spring Boot microservices.

## Features Implemented

### 1. **Rate Limiting** 🚦

Distributed rate limiting using Bucket4j with Redis backend to protect against brute-force attacks, DDoS, and API abuse.

#### Configuration

```yaml
shiva:
  rate-limit:
    enabled: true
    storage: REDIS  # or IN_MEMORY for single-instance
    default-limit:
      capacity: 100
      refill-tokens: 100
      refill-duration: PT1M  # ISO-8601 duration
    whitelisted-ips:
      - "127.0.0.1"
    blacklisted-ips: []
```

#### Usage

Apply to controllers or methods:

```java
// Controller-level (applies to all endpoints)
@RateLimit(capacity = 10, refillTokens = 10, refillDuration = "PT1M", keyType = KeyType.IP)
public class IdentifierController {
    // All endpoints inherit this limit
}

// Method-level (override controller limit)
@PostMapping("/login")
@RateLimit(capacity = 5, refillTokens = 5, refillDuration = "PT5M", keyType = KeyType.IP,
          message = "Too many login attempts. Please try again in 5 minutes.")
public ResponseEntity<?> login(@RequestBody LoginRequest req) { ... }
```

#### Key Types

- **IP**: Rate limit by IP address (default, best for public endpoints)
- **USER**: Rate limit by authenticated user ID
- **GLOBAL**: Global rate limit (all users share the bucket)
- **CUSTOM_HEADER**: Rate limit by custom header value (e.g., API key)

#### Response Headers

When rate limiting is active, responses include:

- `X-Rate-Limit-Remaining`: Tokens remaining
- `X-Rate-Limit-Retry-After-Seconds`: Seconds until refill
- `Retry-After`: Standard HTTP retry header (on 429)

### 2. **Security Headers** 🔒

Comprehensive security headers to protect against common web vulnerabilities (OWASP recommended).

#### Configuration

```yaml
shiva:
  security:
    headers:
      enabled: true
      x-frame-options: true
      x-frame-options-value: DENY  # Prevent clickjacking
      x-content-type-options: true  # Prevent MIME sniffing
      x-xss-protection: true  # Legacy XSS protection
      hsts: true  # Force HTTPS
      hsts-max-age: 31536000  # 1 year
      csp: true  # Content Security Policy
      csp-directives: "default-src 'self'; script-src 'self'..."
```

#### Headers Applied

- `X-Frame-Options`: Prevent clickjacking attacks
- `X-Content-Type-Options`: Prevent MIME type sniffing
- `X-XSS-Protection`: Enable browser XSS protection
- `Strict-Transport-Security` (HSTS): Force HTTPS connections
- `Content-Security-Policy`: Mitigate XSS and injection attacks
- `Referrer-Policy`: Control referrer information leakage
- `Permissions-Policy`: Control browser features (camera, geolocation, etc.)
- `X-Permitted-Cross-Domain-Policies`: Control Flash/PDF policies
- `X-Download-Options`: Prevent IE from executing downloads

### 3. **Request Signature Validation** ✍️

HMAC-based request signing (similar to Laravel's signed routes) for API integrity verification.

#### Configuration

```yaml
shiva:
  security:
    signature:
      enabled: false  # Enable for mobile/external APIs
      secret-key: ${SIGNATURE_SECRET_KEY}
      algorithm: HmacSHA256
```

#### Usage

```java
@PostMapping("/sensitive-operation")
@RequireSignature(expiresIn = 300)  // Valid for 5 minutes
public ResponseEntity<?> sensitiveOperation(@RequestBody DataRequest req) { ... }
```

#### Client Implementation

Clients must include these headers:

```http
X-Signature: <HMAC-SHA256 signature>
X-Timestamp: <Unix timestamp in seconds>
```

**Signature Calculation:**

```javascript
// JavaScript example
const payload = `${method}|${path}|${timestamp}`;
const signature = CryptoJS.HmacSHA256(payload, secretKey).toString(CryptoJS.enc.Base64);
```

### 4. **Enhanced CORS Configuration** 🌐

Environment-specific CORS configuration with proper credentials support.

#### Configuration

```yaml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:4200}
  allow-credentials: true
```

#### Features

- Environment-specific origin whitelisting
- Credential support (cookies, authorization headers)
- Exposed custom headers (`X-Correlation-Id`, rate limit headers)
- Preflight request caching (1 hour)
- Comprehensive allowed headers list

## Applied Rate Limits

### Authentication Endpoints

| Endpoint | Capacity | Refill Duration | Key Type | Reason |
|----------|----------|-----------------|----------|---------|
| `/oauth/identifier/**` | 10 | 1 minute | IP | Prevent user enumeration |
| `/oauth/password` | 5 | 5 minutes | IP | Prevent password brute-force |
| `/oauth/mfa/**` | 3 | 10 minutes | IP | Prevent OTP brute-force |
| Default (all others) | 100 | 1 minute | IP | General API protection |

## Redis Configuration

Rate limiting requires Redis for distributed storage:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
```

## Monitoring and Alerts

### Rate Limit Violations

All rate limit violations are logged:

```java
log.warn("Rate limit exceeded for key: {} on endpoint: {}", key, uri);
```

### Recommendations

1. **Set up alerts** for rate limit violations in your monitoring system
2. **Track rate limit metrics** using Spring Actuator
3. **Implement progressive penalties** for repeated violations
4. **Consider IP blacklisting** for persistent abusers

## Security Best Practices

### Production Deployment

1. **Enable HSTS** with preload for public-facing APIs
2. **Configure strict CSP** based on your frontend requirements
3. **Use HTTPS only** (disable HTTP in production)
4. **Rotate signature secrets** regularly
5. **Monitor rate limit violations** for security incidents
6. **Use Redis Sentinel** or Redis Cluster for high availability

### Environment Variables

Required environment variables:

```bash
# Redis
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# CORS
CORS_ALLOWED_ORIGINS=https://app.yourdomain.com,https://admin.yourdomain.com
CORS_ALLOW_CREDENTIALS=true

# Request Signature (if enabled)
SIGNATURE_SECRET_KEY=your-secret-key-here
```

### Testing Rate Limits

```bash
# Test rate limit
for i in {1..15}; do
  curl -X POST http://localhost:8080/api/v1/oauth/identifier/backoffice \
    -H "Content-Type: application/json" \
    -d '{"identifier":"test@example.com"}'
done

# Should receive 429 Too Many Requests after 10 attempts
```

## API Gateway Considerations

While rate limiting is implemented at the service level, consider these options for future scaling:

### Option 1: Keep Service-Level Rate Limiting
- **Pros**: Granular control, service-specific limits
- **Cons**: Harder to manage across many services

### Option 2: Add API Gateway Layer
- **Pros**: Centralized control, easier monitoring
- **Cons**: Additional infrastructure complexity

### Recommendation

For a microservices architecture with multiple services, implement **both layers**:

1. **API Gateway**: Global rate limiting, authentication, routing
2. **Service Level**: Endpoint-specific limits (current implementation)

Suggested API Gateways:
- **Spring Cloud Gateway**: Native Spring integration
- **Kong**: Enterprise features, plugins
- **AWS API Gateway**: If using AWS infrastructure

## Troubleshooting

### Rate Limit Not Working

1. Check if rate limiting is enabled: `shiva.rate-limit.enabled=true`
2. Verify Redis connection
3. Check logs for initialization errors
4. Ensure `@RateLimit` annotation is present

### Redis Connection Issues

If Redis is unavailable, rate limiting falls back to in-memory buckets (per-instance, not distributed).

### CORS Errors

1. Verify `cors.allowed-origins` includes your frontend URL
2. Check that credentials are properly configured
3. Ensure OPTIONS (preflight) requests are not blocked

## Migration from Laravel

For developers familiar with Laravel, here's a comparison:

| Laravel Feature | Spring Boot Equivalent |
|----------------|------------------------|
| `throttle:60,1` middleware | `@RateLimit(capacity=60, refillDuration="PT1M")` |
| Signed routes | `@RequireSignature` annotation |
| Security headers middleware | `SecurityHeadersFilter` (auto-applied) |
| CORS middleware | CORS configuration in `SecurityConfig` |
| Rate limiting by user | `@RateLimit(keyType=KeyType.USER)` |

## Support

For issues or questions, contact the platform team or refer to:
- Bucket4j Documentation: https://bucket4j.com
- Spring Security: https://spring.io/projects/spring-security
- OWASP Security Headers: https://owasp.org/www-project-secure-headers/

