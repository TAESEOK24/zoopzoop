package com.zoopzoop.zoopzoop.domain.policy.repository;

import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyDetailRepository extends JpaRepository<PolicyDetail, String> {
}