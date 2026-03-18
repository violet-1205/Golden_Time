package com.example.goldentime.dashboard.repository;

import com.example.goldentime.dashboard.entity.GtEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GtEventRepository extends JpaRepository<GtEvent, Long> {
}
