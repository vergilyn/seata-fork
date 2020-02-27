package com.vergilyn.examples.order;

import com.vergilyn.examples.seata.SeataAutoConfiguration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@ImportAutoConfiguration(SeataAutoConfiguration.class)
public class AccountExamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountExamplesApplication.class, args);
    }
}
