package com.example.goldentime.dashboard.repository;

import com.example.goldentime.dashboard.entity.GtOcr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

@Repository
public interface GtOcrRepository extends JpaRepository<GtOcr, Long> {
    Optional<GtOcr> findByGtEvent_GtId(Long gtId);

    /** 존재하는 사건(GtEvent)에 연결된 OCR만 평균 — 사건 삭제 후 고아 행·과거 잔여치 제외 */
    @Query("SELECT AVG(o.confidence) FROM GtOcr o INNER JOIN o.gtEvent e")
    Double getAverageConfidence();
}
