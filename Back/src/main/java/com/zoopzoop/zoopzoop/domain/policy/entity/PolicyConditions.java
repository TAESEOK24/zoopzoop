package com.zoopzoop.zoopzoop.domain.policy.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "policies_conditions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PolicyConditions {
    @Id
    @Column(name = "service_id", length = 100)
    private String serviceId;

    @OneToOne
    @JoinColumn(name = "service_id", insertable = false, updatable = false)
    private PolicyList policyList;

    @Column(nullable = false)
    private String serviceName;

    // 연령 관련
    private Integer ja0110;
    private Integer ja0111;

    // 조건 여부 (VARCHAR)
    @Column(length = 1) private String ja0101;
    @Column(length = 1) private String ja0102;
    @Column(length = 1) private String ja0201;
    @Column(length = 1) private String ja0202;
    @Column(length = 1) private String ja0203;
    @Column(length = 1) private String ja0204;
    @Column(length = 1) private String ja0205;
    @Column(length = 1) private String ja0301;
    @Column(length = 1) private String ja0302;
    @Column(length = 1) private String ja0303;
    @Column(length = 1) private String ja0313;
    @Column(length = 1) private String ja0314;
    @Column(length = 1) private String ja0315;
    @Column(length = 1) private String ja0316;
    @Column(length = 1) private String ja0317;
    @Column(length = 1) private String ja0318;
    @Column(length = 1) private String ja0319;
    @Column(length = 1) private String ja0320;
    @Column(length = 1) private String ja0322;
    @Column(length = 1) private String ja0326;
    @Column(length = 1) private String ja0327;
    @Column(length = 1) private String ja0401;
    @Column(length = 1) private String ja0402;
    @Column(length = 1) private String ja0403;
    @Column(length = 1) private String ja0404;
    @Column(length = 1) private String ja0410;
    @Column(length = 1) private String ja0411;
    @Column(length = 1) private String ja0412;
    @Column(length = 1) private String ja0413;
    @Column(length = 1) private String ja0414;
    @Column(length = 1) private String ja1101;
    @Column(length = 1) private String ja1102;
    @Column(length = 1) private String ja1103;
    @Column(length = 1) private String ja1201;
    @Column(length = 1) private String ja1202;
    @Column(length = 1) private String ja1299;
    @Column(length = 1) private String ja2101;
    @Column(length = 1) private String ja2102;
    @Column(length = 1) private String ja2103;
    @Column(length = 1) private String ja2201;
    @Column(length = 1) private String ja2202;
    @Column(length = 1) private String ja2203;
    @Column(length = 1) private String ja2299;
    @Column(length = 1) private String ja0328;
    @Column(length = 1) private String ja0329;
    @Column(length = 1) private String ja0330;
}