package com.uestcfir;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.uestcfir.mapper")
public class MeetroomManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetroomManagementApplication.class, args);
    }

}
