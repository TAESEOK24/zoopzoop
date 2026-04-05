package com.zoopzoop.zoopzoop.domain.policy.repository;

import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchCriteria;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PolicyListRepositoryCustom {
    Page<PolicyList> searchPolicies(PolicySearchCriteria criteria, Pageable pageable);

    List<String> findServiceTypes(PolicySearchCriteria criteria);
}
