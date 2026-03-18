package com.example.goldentime.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.goldentime.user.entity.UserVehicle;

public interface UserVehicleRepository extends JpaRepository<UserVehicle, Long> {
    List<UserVehicle> findAllByUserUserId(Long userId);
}
