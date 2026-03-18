package com.example.goldentime.user.service;

import com.example.goldentime.user.dto.UserResponseDto;
import com.example.goldentime.user.dto.UserUpdateDto;
import com.example.goldentime.user.dto.VehicleUpdateDto;
import com.example.goldentime.user.entity.User;
import com.example.goldentime.user.entity.UserVehicle;
import com.example.goldentime.user.repository.UserRepository;
import com.example.goldentime.user.repository.UserVehicleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserVehicleRepository userVehicleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserVehicleRepository userVehicleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userVehicleRepository = userVehicleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponseDto findByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId)
                .map(UserResponseDto::new)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + loginId));
    }

    @Transactional
    public void updateProfile(String loginId, UserUpdateDto updateDto) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + loginId));

        user.setUserName(updateDto.getUserName());
        user.setPhone(updateDto.getPhone());
        user.setEmail(updateDto.getEmail());
        user.setAddress(updateDto.getAddress());

        if (updateDto.getPassword() != null && !updateDto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updateDto.getPassword()));
        }

        // 차량 정보 업데이트
        if (updateDto.getVehicles() != null) {
            for (VehicleUpdateDto vehicleDto : updateDto.getVehicles()) {
                UserVehicle vehicle = userVehicleRepository.findById(vehicleDto.getVehicleId())
                        .orElseThrow(() -> new IllegalArgumentException("차량 정보를 찾을 수 없습니다: " + vehicleDto.getVehicleId()));
                
                // 해당 차량이 현재 사용자의 차량인지 확인 (보안)
                if (!vehicle.getUser().getUserId().equals(user.getUserId())) {
                    throw new SecurityException("권한이 없는 차량 정보 수정 시도입니다.");
                }

                vehicle.setCarNumber(vehicleDto.getCarNumber());
                vehicle.setSerialNumber(vehicleDto.getSerialNumber());
                userVehicleRepository.save(vehicle);
            }
        }

        userRepository.save(user);
    }
}
