package com.zoopzoop.zoopzoop.global.security.jwt;

import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    public String issuePlaceholderToken() {
        return "placeholder-token";
    }
}
