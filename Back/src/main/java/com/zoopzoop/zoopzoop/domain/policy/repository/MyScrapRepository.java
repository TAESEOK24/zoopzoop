package com.zoopzoop.zoopzoop.domain.policy.repository;

import com.zoopzoop.zoopzoop.domain.policy.entity.MyScrap;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MyScrapRepository extends JpaRepository<MyScrap, Long> {

    @Query("""
            select count(s) > 0
            from MyScrap s
            where s.user.id = :userId
              and s.policy.serviceId = :serviceId
            """)
    boolean existsScrap(
            @Param("userId") Long userId,
            @Param("serviceId") String serviceId
    );

    @Modifying
    @Query("""
            delete from MyScrap s
            where s.user.id = :userId
              and s.policy.serviceId = :serviceId
            """)
    int deleteScrap(
            @Param("userId") Long userId,
            @Param("serviceId") String serviceId
    );

    @Query(
            value = """
            select s
            from MyScrap s
            join fetch s.policy p
            where s.user.id = :userId
            order by s.createdAt desc
            """,
            countQuery = """
            select count(s)
            from MyScrap s
            where s.user.id = :userId
            """
    )
    Page<MyScrap> findMyScrapsByRecent(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query(
            value = """
            select s
            from MyScrap s
            join fetch s.policy p
            where s.user.id = :userId
              and (
                    lower(coalesce(p.serviceName, '')) like concat('%', :query, '%')
                    or lower(coalesce(p.orgName, '')) like concat('%', :query, '%')
                    or lower(coalesce(p.departmentName, '')) like concat('%', :query, '%')
                  )
            order by s.createdAt desc
            """,
            countQuery = """
            select count(s)
            from MyScrap s
            join s.policy p
            where s.user.id = :userId
              and (
                    lower(coalesce(p.serviceName, '')) like concat('%', :query, '%')
                    or lower(coalesce(p.orgName, '')) like concat('%', :query, '%')
                    or lower(coalesce(p.departmentName, '')) like concat('%', :query, '%')
                  )
            """
    )
    Page<MyScrap> searchMyScrapsByRecent(
            @Param("userId") Long userId,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("""
            select s.policy.serviceId
            from MyScrap s
            where s.user.id = :userId
            """)
    List<String> findPolicyIdsByUserId(@Param("userId") Long userId);

    @Query("""
            select s
            from MyScrap s
            join fetch s.user
            join fetch s.policy
            where s.user.id in :userIds
            order by s.user.id asc, s.createdAt desc
            """)
    List<MyScrap> findByUserIdsWithPolicy(@Param("userIds") List<Long> userIds);
}
