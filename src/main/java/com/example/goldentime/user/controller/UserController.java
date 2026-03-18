package com.example.goldentime.user.controller;

import com.example.goldentime.car.dto.CarResponseDto;
import com.example.goldentime.user.repository.UserVehicleRepository;
import com.example.goldentime.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserVehicleRepository userVehicleRepository;

    public UserController(UserService userService, UserVehicleRepository userVehicleRepository) {
        this.userService = userService;
        this.userVehicleRepository = userVehicleRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "users/list";
    }

    @GetMapping("/{userId}/vehicles")
    @ResponseBody
    public ResponseEntity<List<CarResponseDto>> getUserVehicles(@PathVariable Long userId) {
        List<CarResponseDto> vehicles = userVehicleRepository.findAllByUserUserId(userId).stream()
                .map(CarResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(vehicles);
    }
}
