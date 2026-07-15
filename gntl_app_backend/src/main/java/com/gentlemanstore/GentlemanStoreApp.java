package com.gentlemanstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GentlemanStoreApp {
     public static void main(String[] args){
         SpringApplication.run(GentlemanStoreApp.class, args);
     }
}
