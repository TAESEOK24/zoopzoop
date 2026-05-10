package com.zoopzoop.zoopzoop.domain.searchlog.repository;

import com.zoopzoop.zoopzoop.domain.searchlog.entity.SearchLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchLogRepository extends JpaRepository<SearchLog, Integer> {
    List<SearchLog> findTop30ByUserIdAndActionTypeOrderByActionTimeDesc(Integer userId, String actionType);

    void deleteByUserId(Integer userId);
}
