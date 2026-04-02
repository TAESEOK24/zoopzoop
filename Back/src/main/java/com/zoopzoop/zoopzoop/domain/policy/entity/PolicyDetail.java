package com.zoopzoop.zoopzoop.domain.policy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "policies_detail")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PolicyDetail {
    @Id
    @Column(name = "service_id", length = 100)
    private String serviceId;

    @OneToOne
    @JoinColumn(name = "service_id", insertable = false, updatable = false)
    private PolicyList policyList;

    // 💡 에러 방지를 위해 모든 String 필드를 TEXT 타입으로 넉넉하게 지정합니다.
    @Column(columnDefinition = "TEXT") private String purpose;
    @Column(columnDefinition = "TEXT") private String requiredDocuments;
    @Column(columnDefinition = "TEXT") private String receivingOrgName;
    @Column(columnDefinition = "TEXT") private String contactInfo;
    @Column(columnDefinition = "TEXT") private String onlineUrl;
    @Column(columnDefinition = "TEXT") private String orgName;
    @Column(columnDefinition = "TEXT") private String adminRule;
    @Column(columnDefinition = "TEXT") private String localRule;
    @Column(columnDefinition = "TEXT") private String law;
    @Column(columnDefinition = "TEXT") private String officialRequiredDocs;
    @Column(columnDefinition = "TEXT") private String userRequiredDocs;

    private LocalDateTime updatedAt;
}