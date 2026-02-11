package edu.jmaycon.cdcapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CdcApplication {

    static {
        System.setProperty("aws.region", "eu-central-1");
        System.setProperty("aws.accessKeyId", "admin");
        System.setProperty("aws.secretAccessKey", "admin123");
    }

    public static void main(String[] args) {
        SpringApplication.run(CdcApplication.class, args);
    }
}
