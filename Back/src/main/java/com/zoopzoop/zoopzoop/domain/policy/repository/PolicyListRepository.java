package com.zoopzoop.zoopzoop.domain.policy.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;

public interface PolicyListRepository extends JpaRepository<PolicyList, String>, PolicyListRepositoryCustom {
}
