package ke.shiva.sbs_iam.modules.iam.shared.exception;


public class DomainNotAllowedException extends RuntimeException {
    public DomainNotAllowedException(String message) {
        super(message);
    }
}
