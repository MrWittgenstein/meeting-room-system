package com.uestcfir;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.uestcfir.config.PlatformServiceProperties;
import com.uestcfir.messaging.MeetingEventProperties;
import com.uestcfir.cache.CacheInvalidationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.uestcfir.mapper")
@EnableConfigurationProperties({PlatformServiceProperties.class, MeetingEventProperties.class, CacheInvalidationProperties.class})
public class MeetroomManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetroomManagementApplication.class, args);
    }

}
