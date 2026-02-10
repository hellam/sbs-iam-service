package ke.shiva.sbs_iam;

import ke.shiva.client.notification.config.NotificationClientAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;

@ConfigurationPropertiesScan
@SpringBootApplication
public class SbsIAMApplication {

    public static void main(String[] args) {
        SpringApplication.run(SbsIAMApplication.class, args);
    }
}
