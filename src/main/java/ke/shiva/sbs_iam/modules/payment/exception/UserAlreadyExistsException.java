package ke.shiva.sbs_iam.modules.payment.exception;

import ke.shiva.shivacorestarter.constants.ErrorCodes;
import ke.shiva.shivacorestarter.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BaseException {
    public UserAlreadyExistsException() {
        super(ErrorCodes.DUPLICATE_ENTRY + "User already exists", HttpStatus.CONFLICT);
    }
}

