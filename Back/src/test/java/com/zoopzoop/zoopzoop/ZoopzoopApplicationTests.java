package com.zoopzoop.zoopzoop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestChatbotConfig.class)
class ZoopzoopApplicationTests {

    @Test
    void contextLoads() {
    }

}
