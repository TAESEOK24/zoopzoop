package com.zoopzoop.zoopzoop.domain.policy.repository;

import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyListRepository extends JpaRepository<PolicyList, String> {

    @Query("""
            select p
            from PolicyList p
            where lower(coalesce(p.serviceName, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(p.purposeSummary, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(p.target, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(p.supportContent, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(p.applicationMethod, '')) like lower(concat('%', :keyword, '%'))
            order by p.viewCount desc, p.createdAt desc
            """)
    List<PolicyList> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
