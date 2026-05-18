package com.zoopzoop.zoopzoop.domain.community.repository;

import com.zoopzoop.zoopzoop.domain.community.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatusOrderByIdDesc(String status);
}