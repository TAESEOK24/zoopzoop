package com.zoopzoop.zoopzoop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestChatbotConfig.class, TestSecurityConfig.class})
class ZoopzoopApplicationTests {

    @Test
    void contextLoads() {
    }

}
