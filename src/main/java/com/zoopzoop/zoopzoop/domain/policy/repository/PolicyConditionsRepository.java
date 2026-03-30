package com.zoopzoop.zoopzoop.domain.policy.repository;

import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyConditions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyConditionsRepository extends JpaRepository<PolicyConditions, String> {
}