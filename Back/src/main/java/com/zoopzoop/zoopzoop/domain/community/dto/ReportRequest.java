package com.zoopzoop.zoopzoop.domain.community.dto;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportRequest {
    private String targetType;
    private Long targetId;
    private String reason;
}