package com.example.goldentime.dashboard.repository;

import com.example.goldentime.dashboard.entity.GtOcr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GtOcrRepository extends JpaRepository<GtOcr, Long> {
    List<GtOcr> findByGtEvent_GtId(Long gtId);
    List<GtOcr> findByGtEvent_GtIdAndIsFinalTrue(Long gtId);
}
