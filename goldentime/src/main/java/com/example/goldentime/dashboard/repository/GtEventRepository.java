package com.example.goldentime.dashboard.repository;

import com.example.goldentime.dashboard.entity.GtEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.time.LocalDateTime;

@Repository
public interface GtEventRepository extends JpaRepository<GtEvent, Long> {

    @Query("SELECT e FROM GtEvent e JOIN FETCH e.vehicle v JOIN FETCH v.user")
    @Override
    List<GtEvent> findAll();

    long countByCreatedAtAfter(LocalDateTime startOfDay);
    long countByCreatedAtAfterAndSentToFireTrue(LocalDateTime startOfDay);
    long countByCreatedAtAfterAndSentToSafetyTrue(LocalDateTime startOfDay);

    List<GtEvent> findTop5ByOrderByCreatedAtDesc();
    // 로그인 사용자의 차량(등록 차량) 기준으로 최근 5건을 조회
    List<GtEvent> findTop5ByVehicle_User_UserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT e.vtIdPath FROM GtEvent e WHERE e.vtIdPath IS NOT NULL")
    List<String> findAllVtIdPaths();
}
