package com.example.goldentime.user.dto;

import com.example.goldentime.car.dto.CarResponseDto;
import com.example.goldentime.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class UserResponseDto {
    private Long userId;
    private String loginId;
    private String userName;
    private String phone;
    private String email;
    private String role;
    private String address;
    private LocalDateTime createdAt;
    private List<CarResponseDto> vehicles;

    public UserResponseDto(User entity) {
        this.userId = entity.getUserId();
        this.loginId = entity.getLoginId();
        this.userName = entity.getUserName();
        this.phone = entity.getPhone();
        this.email = entity.getEmail();
        this.role = entity.getRole();
        this.address = entity.getAddress();
        this.createdAt = entity.getCreatedAt();
        this.vehicles = entity.getVehicles().stream()
                .map(CarResponseDto::new)
                .collect(Collectors.toList());
    }
}
