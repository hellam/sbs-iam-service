package ke.shiva.sbs_iam.config.SecurityConfig;

public final class SecurityConstants {

    private SecurityConstants() {
        // Private constructor to prevent instantiation
    }

    //Header Constants
    public static final class Headers {
        private Headers() {
        }

        public static final String FLOW_ID_HEADER = "X-Flow-ID";
        public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
        public static final String REQUEST_ID_HEADER = "X-Request-ID";
        public static final String AUTHORIZATION_HEADER = "Authorization";
        public static final String DEVICE_TYPE = "X-Device-Type";
        public static final String DEVICE_ID = "X-Device-ID";
        public static final String PLATFORM = "X-Platform";
        public static final String BROWSER = "X-Browser";
        public static final String BROWSER_VERSION = "X-Browser-Version";
        public static final String USER_AGENT_HEADER = "User-Agent";
    }
}
