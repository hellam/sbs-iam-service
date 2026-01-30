# GeoIP Configuration Guide

## Overview
The IAM service now uses MaxMind GeoIP2 for automatic IP geolocation to determine the country and city of login attempts.

## Setup Instructions

### 1. Download MaxMind GeoIP2 Database

You need to download the MaxMind GeoLite2 or GeoIP2 City database:

#### Option A: GeoLite2 (Free)
1. Sign up for a free MaxMind account at: https://www.maxmind.com/en/geolite2/signup
2. Log in and navigate to: https://www.maxmind.com/en/accounts/current/geoip/downloads
3. Download **GeoLite2 City** in **Binary / gzip** format (`.mmdb` file)
4. Extract the `.mmdb` file

#### Option B: GeoIP2 (Paid - More Accurate)
1. Purchase GeoIP2 City subscription from MaxMind
2. Download the GeoIP2 City database (`.mmdb` file)

### 2. Place Database File

Place the database file in your preferred location. Recommended locations:

- **Development**: `/usr/local/share/GeoIP/GeoLite2-City.mmdb`
- **Production**: `/opt/geoip/GeoLite2-City.mmdb` or use volume mount in Docker

### 3. Configure Application

Add the following property to your application configuration:

#### application.yaml
```yaml
geoip:
  database:
    path: /usr/local/share/GeoIP/GeoLite2-City.mmdb
```

#### application-dev.yaml
```yaml
geoip:
  database:
    path: /usr/local/share/GeoIP/GeoLite2-City.mmdb
```

#### application-prod.yaml
```yaml
geoip:
  database:
    path: ${GEOIP_DATABASE_PATH:/opt/geoip/GeoLite2-City.mmdb}
```

#### Environment Variable (Docker/K8s)
```bash
GEOIP_DATABASE_PATH=/opt/geoip/GeoLite2-City.mmdb
```

### 4. Docker Setup (Optional)

#### Dockerfile
```dockerfile
FROM openjdk:17-slim

# Create directory for GeoIP database
RUN mkdir -p /opt/geoip

# Copy GeoIP database
COPY GeoLite2-City.mmdb /opt/geoip/

# Copy application jar
COPY target/iam-service-*.jar /app/app.jar

WORKDIR /app
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Docker Compose
```yaml
services:
  iam-service:
    image: iam-service:latest
    environment:
      - GEOIP_DATABASE_PATH=/opt/geoip/GeoLite2-City.mmdb
    volumes:
      - ./geoip:/opt/geoip:ro
```

#### Kubernetes ConfigMap
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: geoip-config
data:
  application.yaml: |
    geoip:
      database:
        path: /opt/geoip/GeoLite2-City.mmdb
```

## Features

### Automatic Location Detection
- IP addresses are automatically resolved to geographic locations
- Country code (2-letter ISO code) is stored in `login_history.location_country`
- City name is stored in `login_history.location_city`

### Fallback Mechanism
- If GeoIP database is not configured, the service continues to work without location data
- Manual location headers (`X-Country-Code`, `X-City`) take precedence over GeoIP lookup
- Private/local IP addresses (10.x.x.x, 192.168.x.x, 127.0.0.1) are skipped for GeoIP lookup

### Performance
- GeoIP lookups are fast (< 1ms typically)
- Database is loaded into memory at startup
- No external API calls - all lookups are local

## Data Captured

For each login attempt, the GeoIP service provides:
- **Country Name**: Full country name (e.g., "Kenya", "United States")
- **Country Code**: ISO 3166-1 alpha-2 code (e.g., "KE", "US")
- **City**: City name (e.g., "Nairobi", "New York")
- **Region**: State/Province (optional)
- **Latitude/Longitude**: Coordinates (optional, for future use)
- **Time Zone**: IANA time zone (optional)

## Database Updates

MaxMind releases new databases regularly:
- **GeoLite2**: Updated twice weekly (free)
- **GeoIP2**: Updated daily (paid)

### Manual Update Process
1. Download the latest database from MaxMind
2. Replace the old `.mmdb` file
3. Restart the IAM service (or use hot-reload if implemented)

### Automated Update (Recommended)
Use MaxMind's `geoipupdate` tool to automatically update the database:

```bash
# Install geoipupdate
brew install geoipupdate  # macOS
apt-get install geoipupdate  # Ubuntu

# Configure with your account credentials
cat > /usr/local/etc/GeoIP.conf <<EOF
AccountID YOUR_ACCOUNT_ID
LicenseKey YOUR_LICENSE_KEY
EditionIDs GeoLite2-City
DatabaseDirectory /usr/local/share/GeoIP
EOF

# Set up cron job for weekly updates
crontab -e
# Add: 0 2 * * 3 /usr/local/bin/geoipupdate
```

## Testing

### Test with Real IP
```bash
# Check login from a specific location
curl -X POST http://localhost:8080/oauth/identifier/mobile \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: 41.90.64.0" \
  -d '{"identifier": "test@example.com"}'
```

### Check Logs
```bash
# Look for GeoIP log messages
grep "GeoIP" logs/app.log
```

Expected log output:
```
[INFO] GeoIP database loaded successfully from: /usr/local/share/GeoIP/GeoLite2-City.mmdb
[DEBUG] GeoIP lookup successful for IP 41.90.64.0: Nairobi, Kenya
```

## Troubleshooting

### Issue: "GeoIP database file not found"
**Solution**: Verify the file path in your configuration matches the actual file location.

### Issue: "GeoIP lookup failed"
**Reasons**:
- Invalid IP address format
- IP is private/local (10.x, 192.168.x, 127.0.0.1)
- IP not found in database (rare for public IPs)

### Issue: Location always null
**Check**:
1. Is the database file configured correctly?
2. Is the IP address valid and public?
3. Check application logs for errors

### Issue: High memory usage
**Solution**: The GeoIP2 City database is ~70MB and loaded into memory. This is normal. Consider using the Country database (~6MB) if city-level accuracy is not required.

## Privacy Considerations

- GeoIP data is approximate and based on IP allocation
- Accuracy varies: Country (~99%), City (~80-95%)
- No personal information is sent to MaxMind - all lookups are local
- Comply with GDPR/privacy laws when storing location data
- Consider anonymizing or aggregating location data for analytics

## Cost

- **GeoLite2**: Free (less accurate, updated bi-weekly)
- **GeoIP2 Precision**: Paid subscription (more accurate, updated daily)
  - City: $60/month or $1,500/year (10,000 queries/month)
  - Unlimited: Custom pricing

For most applications, GeoLite2 is sufficient.

## Additional Resources

- MaxMind Documentation: https://dev.maxmind.com/geoip/docs/databases
- GeoIP2 Java API: https://github.com/maxmind/GeoIP2-java
- MaxMind Account: https://www.maxmind.com/en/account
- Database Downloads: https://www.maxmind.com/en/accounts/current/geoip/downloads

