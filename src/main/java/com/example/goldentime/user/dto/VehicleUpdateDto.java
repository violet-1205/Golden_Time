package com.example.goldentime.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VehicleUpdateDto {
    private Long vehicleId;
    private String carNumber;
    private String serialNumber;
}
