package com.zoopzoop.zoopzoop.domain.policy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "policies_list")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PolicyList {

    @Id
    @Column(name = "service_id", length = 100)
    private String serviceId;

    // 💡 모든 문자열 데이터를 넉넉하게 TEXT로 변경! (255자 에러 원천 차단)
    @Column(columnDefinition = "TEXT") private String serviceType;
    @Column(columnDefinition = "TEXT") private String serviceName;
    @Column(columnDefinition = "TEXT") private String purposeSummary;
    @Column(columnDefinition = "TEXT") private String target;
    @Column(columnDefinition = "TEXT") private String selectionCriteria;
    @Column(columnDefinition = "TEXT") private String supportContent;
    @Column(columnDefinition = "TEXT") private String applicationMethod;
    @Column(columnDefinition = "TEXT") private String applicationDeadline;
    @Column(columnDefinition = "TEXT") private String detailUrl;

    @Column(columnDefinition = "TEXT") private String orgCode;
    @Column(columnDefinition = "TEXT") private String orgName;
    @Column(columnDefinition = "TEXT") private String departmentName;

    @Column(nullable = false) private Integer viewCount;

    @Column(columnDefinition = "TEXT") private String orgType;
    @Column(columnDefinition = "TEXT") private String userType;
    @Column(columnDefinition = "TEXT") private String serviceField;
    @Column(columnDefinition = "TEXT") private String receivingOrg;
    @Column(columnDefinition = "TEXT") private String contactNumber;

    @Column(nullable = false) private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}