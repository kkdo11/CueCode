package kopo.motionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MotionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MotionServiceApplication.class, args);
    }

}

