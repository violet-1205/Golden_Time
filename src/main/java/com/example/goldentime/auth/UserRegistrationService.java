package com.example.goldentime.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.goldentime.auth.dto.SignupForm;
import com.example.goldentime.user.entity.User;
import com.example.goldentime.user.entity.UserVehicle;
import com.example.goldentime.user.repository.UserRepository;
import com.example.goldentime.user.repository.UserVehicleRepository;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final UserVehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(
            UserRepository userRepository,
            UserVehicleRepository vehicleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(SignupForm form) {
        if (userRepository.existsByLoginId(form.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        User user = new User();
        user.setLoginId(form.getLoginId());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setUserName(form.getUserName());
        user.setPhone(form.getPhone());
        user.setEmail(form.getEmail());
        user.setRole("USER");
        user.setAddress(form.getAddress());

        User savedUser = userRepository.save(user);

        // 차량 정보 함께 저장
        UserVehicle vehicle = new UserVehicle();
        vehicle.setUser(savedUser);
        vehicle.setCarNumber(form.getCarNumber());
        vehicle.setSerialNumber(form.getSerialNumber());
        vehicleRepository.save(vehicle);

        return savedUser;
    }
}
