package com.zoopzoop.zoopzoop.domain.admin.dto;

import com.zoopzoop.zoopzoop.domain.community.entity.Report;

public record AdminReportResponse(
        Long id,
        String targetType,
        Long targetId,
        String reason,
        String reporter,
        String status,
        String createdAt
) {
    public static AdminReportResponse from(Report report) {
        return new AdminReportResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getReporter(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}