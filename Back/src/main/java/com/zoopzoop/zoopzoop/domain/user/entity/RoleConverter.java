package com.zoopzoop.zoopzoop.domain.user.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role attribute) {
        if (attribute == null) {
            return null;
        }
        // DB에 저장할 때는 기본적으로 이름(대문자)으로 저장
        return attribute.name();
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // 🚀 핵심: DB에서 읽어온 값이 소문자(user)든 대문자(USER)든 무조건 대문자로 변환하여 Enum과 매칭
        try {
            return Role.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 알 수 없는 값이 들어있을 경우 기본값으로 USER 부여 (또는 에러 처리 가능)
            return Role.USER;
        }
    }
}