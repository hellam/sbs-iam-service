package ke.shiva.microservice_template.modules.notification;

import ke.shiva.shivacorestarter.security.PasswordEncoderFactory;
import ke.shiva.shivacorestarter.util.HashUtil;
import org.springframework.beans.factory.annotation.Value;

public class Test {
    //Generate Main class for test module
    public static void main(String[] args) {
        System.out.println(HashUtil.bcrypt("Shiva"));
        System.out.println(HashUtil.bcryptVerify("Shiva", "$2a$10$AKR7q.W9Kwrk3.WsfrhYvOoyfKF.GZhFQNq4M/axcxej3jGlJnCdy"));
    }
}
