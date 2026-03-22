package com.zoopzoop.zoopzoop.boundedcontext.user.out;

import org.springframework.stereotype.Component;

@Component
public class UserClient {

    public String getStatus() {
        return "ready";
    }
}
