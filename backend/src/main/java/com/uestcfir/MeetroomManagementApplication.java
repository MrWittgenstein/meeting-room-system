package com.uestcfir;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.uestcfir.config.PlatformServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
@MapperScan("com.uestcfir.mapper")
@EnableConfigurationProperties(PlatformServiceProperties.class)
public class MeetroomManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetroomManagementApplication.class, args);
    }

}
