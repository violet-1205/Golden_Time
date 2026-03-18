package com.example.goldentime.dashboard.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.example.goldentime.user.entity.UserVehicle;

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

import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "TB_GT_EVENT", schema = "user_management")
public class GtEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gt_id")
    private Long gtId;

    @OneToMany(mappedBy = "gtEvent", fetch = FetchType.LAZY)
    private List<GtOcr> ocrResults = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private UserVehicle vehicle;

    @Column(name = "video_path")
    private String videoPath;

    @Column(name = "latitude", precision = 13, scale = 10)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 13, scale = 10)
    private BigDecimal longitude;

    @Column(name = "status")
    private String status;

    @Column(name = "ocr_attempted")
    private boolean ocrAttempted;

    @Column(name = "exposure_duration")
    private Short exposureDuration;

    @Column(name = "clip_start_offset")
    private Short clipStartOffset;

    @Column(name = "clip_end_offset")
    private Short clipEndOffset;

    @Column(name = "sent_to_fire")
    private boolean sentToFire;

    @Column(name = "sent_to_safety")
    private boolean sentToSafety;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
