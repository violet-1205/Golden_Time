package com.example.goldentime.car.dto;

import com.example.goldentime.user.entity.UserVehicle;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CarResponseDto {
    private Long vehicleId;
    private String carNumber;
    private String serialNumber;
    private String ownerName;
    private String ownerLoginId;
    private LocalDateTime createdAt;

    public CarResponseDto(UserVehicle entity) {
        this.vehicleId = entity.getVehicleId();
        this.carNumber = entity.getCarNumber();
        this.serialNumber = entity.getSerialNumber();
        this.ownerName = entity.getUser().getUserName();
        this.ownerLoginId = entity.getUser().getLoginId();
        this.createdAt = entity.getCreatedAt();
    }
}
