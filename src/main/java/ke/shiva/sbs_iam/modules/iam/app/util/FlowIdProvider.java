package ke.shiva.sbs_iam.modules.iam.app.util;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.shivacorestarter.exception.BaseException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

@Component
@RequestScope
public class FlowIdProvider {

    private final HttpServletRequest request;

    public FlowIdProvider(HttpServletRequest request) {
        this.request = request;
    }

    public UUID getFlowId() {
        String flowIdHeader = request.getHeader("X-Flow-ID");
        if (flowIdHeader == null || flowIdHeader.isBlank()) {
            throw BaseException.invalidFlow("X-Flow-ID header is missing.");
        }
        try {
            return UUID.fromString(flowIdHeader);
        } catch (IllegalArgumentException e) {
            throw BaseException.invalidFlow("Invalid X-Flow-ID format.");
        }
    }
}

