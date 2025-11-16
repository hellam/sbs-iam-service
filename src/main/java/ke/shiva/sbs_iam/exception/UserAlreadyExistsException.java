package ke.shiva.sbs_iam.exception;

import ke.shiva.shivacorestarter.constants.ErrorCodes;
import ke.shiva.shivacorestarter.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BaseException {
    public UserAlreadyExistsException() {
        super(ErrorCodes.DUPLICATE_ENTRY + "User already exists", HttpStatus.CONFLICT);
    }
}

