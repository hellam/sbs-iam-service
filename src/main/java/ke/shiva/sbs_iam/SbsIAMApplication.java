package ke.shiva.sbs_iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class SbsIAMApplication {

    public static void main(String[] args) {
        SpringApplication.run(SbsIAMApplication.class, args);
    }
}
