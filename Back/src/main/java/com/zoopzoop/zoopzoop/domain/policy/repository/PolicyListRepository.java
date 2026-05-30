package com.zoopzoop.zoopzoop.domain.policy.repository;

import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyListRepository extends JpaRepository<PolicyList, String>, PolicyListRepositoryCustom {

    @Query("""
            select p
            from PolicyList p
            where lower(coalesce(p.serviceName, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(p.purposeSummary, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(p.target, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(p.supportContent, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(p.applicationMethod, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(p.serviceType, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(p.orgName, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(p.departmentName, '')) like lower(concat('%', :keyword, '%'))
            order by p.viewCount desc, p.createdAt desc
            """)
    List<PolicyList> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            select p
            from PolicyList p
            where (
                   lower(coalesce(p.serviceName, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.purposeSummary, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.target, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.supportContent, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.applicationMethod, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.serviceType, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.orgName, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.departmentName, '')) like lower(concat('%', :keyword, '%'))
            )
              and exists (
                select 1
                from PolicyConditions c
                where c.serviceId = p.serviceId
                  and (c.ja0110 is null or c.ja0110 <= :age)
                  and (c.ja0111 is null or c.ja0111 >= :age)
              )
            order by p.viewCount desc, p.createdAt desc
            """)
    List<PolicyList> searchByKeywordAndAge(
            @Param("keyword") String keyword,
            @Param("age") Integer age,
            Pageable pageable
    );

    List<PolicyList> findByServiceTypeContainingIgnoreCase(String serviceType, Pageable pageable);

    @Query("""
            select p
            from PolicyList p
            where lower(coalesce(p.orgName, '')) like lower(concat('%', :organization, '%'))
               or lower(coalesce(p.departmentName, '')) like lower(concat('%', :organization, '%'))
            order by p.viewCount desc, p.createdAt desc
            """)
    List<PolicyList> searchByOrganization(@Param("organization") String organization, Pageable pageable);

    List<PolicyList> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAfter, Pageable pageable);
}
