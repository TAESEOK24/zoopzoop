package com.zoopzoop.zoopzoop.domain.policy.repository;

import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyListRepository extends JpaRepository<PolicyList, String> {
}