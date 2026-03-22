package com.zoopzoop.zoopzoop.global.auth.jwt;

import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    public String issuePlaceholderToken() {
        return "placeholder-token";
    }
}
