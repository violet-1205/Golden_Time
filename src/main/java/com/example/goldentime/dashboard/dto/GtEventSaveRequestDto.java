package com.example.goldentime.dashboard.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class GtEventSaveRequestDto {
    private Long vehicleId;
    private MultipartFile videoFile;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String status = "processing";
    private boolean ocrAttempted = true;
    private Short exposureDuration = 10;
}
