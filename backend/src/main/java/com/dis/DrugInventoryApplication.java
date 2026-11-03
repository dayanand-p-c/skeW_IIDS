package com.dis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication
@EnableScheduling
public class DrugInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrugInventoryApplication.class, args);
    }
}
