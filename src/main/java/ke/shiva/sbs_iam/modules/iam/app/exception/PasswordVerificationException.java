package ke.shiva.sbs_iam.modules.iam.app.exception;

import ke.shiva.shivacorestarter.exception.BaseException;
import org.springframework.http.HttpStatus;

public class PasswordVerificationException extends BaseException {

    private PasswordVerificationException(String message, HttpStatus status, Object data) {
        super(message, status, data);
    }

    public static PasswordVerificationException invalidCredentials(String message, Object data) {
        return new PasswordVerificationException(message, HttpStatus.BAD_REQUEST, data);
    }
}
