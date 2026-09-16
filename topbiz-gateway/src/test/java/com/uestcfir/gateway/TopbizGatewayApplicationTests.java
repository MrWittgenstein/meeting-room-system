package com.uestcfir.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "MEETINGROOM_SERVICE_URL=http://127.0.0.1:8081")
class TopbizGatewayApplicationTests {
    @Test
    void contextLoads() {
    }
}
