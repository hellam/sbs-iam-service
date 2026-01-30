package ke.shiva.sbs_iam.modules.iam.app.security;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import ke.shiva.sbs_iam.config.SecurityConfig.SecurityConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Hidden
@Parameter(
        in = ParameterIn.HEADER,
        name = SecurityConstants.Headers.FLOW_ID_HEADER,
        description = "The unique identifier for the login flow.",
        required = true,
        schema = @Schema(type = "string", format = "uuid")
)
public @interface FlowId {
}

