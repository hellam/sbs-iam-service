package ke.shiva.sbs_iam.modules.iam.app.security;

import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresStage {
    LoginStage value();
}

