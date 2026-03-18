package com.example.goldentime.dashboard.dto;

import com.example.goldentime.dashboard.entity.GtEvent;
import com.example.goldentime.dashboard.entity.GtOcr;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class GtEventResponseDto {
    private Long gtId;
    private Long vehicleId;
    private String carNumber;
    private String videoPath;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String status;
    private boolean ocrAttempted;
    private Short exposureDuration;
    private Short clipStartOffset;
    private Short clipEndOffset;
    private boolean sentToFire;
    private boolean sentToSafety;
    private LocalDateTime createdAt;
    
    // OCR 정보 추가
    private String detectedPlate;
    private Double confidence;

    public GtEventResponseDto(GtEvent entity) {
        this.gtId = entity.getGtId();
        this.vehicleId = entity.getVehicle().getVehicleId();
        this.carNumber = entity.getVehicle().getCarNumber();
        this.videoPath = entity.getVideoPath();
        this.latitude = entity.getLatitude();
        this.longitude = entity.getLongitude();
        this.status = entity.getStatus();
        this.ocrAttempted = entity.isOcrAttempted();
        this.exposureDuration = entity.getExposureDuration();
        this.clipStartOffset = entity.getClipStartOffset();
        this.clipEndOffset = entity.getClipEndOffset();
        this.sentToFire = entity.isSentToFire();
        this.sentToSafety = entity.isSentToSafety();
        this.createdAt = entity.getCreatedAt();
        
        // 최종 OCR 결과 매핑 (있을 경우)
        entity.getOcrResults().stream()
            .filter(GtOcr::isFinal)
            .findFirst()
            .ifPresent(ocr -> {
                this.detectedPlate = ocr.getDetectedPlate();
                this.confidence = ocr.getConfidence();
            });
    }
}
