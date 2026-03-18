package com.example.goldentime.dashboard.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "TB_GT_OCR", schema = "user_management")
public class GtOcr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ocr_id")
    private Long ocrId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gt_id", nullable = false)
    private GtEvent gtEvent;

    @Column(name = "detected_plate")
    private String detectedPlate;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "is_final")
    private boolean isFinal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
