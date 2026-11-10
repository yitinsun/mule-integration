package com.yitinsun.muleintegration;

import com.yitinsun.muleintegration.config.MuleProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MuleProperties.class)
public class PublicApiEdgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublicApiEdgeApplication.class, args);
    }
}
