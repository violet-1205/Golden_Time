package com.example.goldentime.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdateDto {
    private String userName;
    private String phone;
    private String email;
    private String address;
    private String password; // 선택적 수정
    private List<VehicleUpdateDto> vehicles;
}
