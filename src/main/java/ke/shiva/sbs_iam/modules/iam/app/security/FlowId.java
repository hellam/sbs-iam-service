package ke.shiva.sbs_iam.modules.iam.app.security;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(
        in = ParameterIn.HEADER,
        name = "X-Flow-ID",
        description = "The unique identifier for the login flow.",
        required = true,
        schema = @Schema(type = "string", format = "uuid")
)
public @interface FlowId {
}

