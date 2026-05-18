package com.zoopzoop.zoopzoop.domain.community.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequest {
    private String targetType;
    private Long targetId;
    private String reason;
}