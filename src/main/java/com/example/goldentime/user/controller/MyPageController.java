package com.example.goldentime.user.controller;

import com.example.goldentime.user.dto.UserResponseDto;
import com.example.goldentime.user.dto.UserUpdateDto;
import com.example.goldentime.user.dto.VehicleUpdateDto;
import com.example.goldentime.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/mypage")
public class MyPageController {

    private final UserService userService;

    public MyPageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String myPage(Principal principal, Model model) {
        UserResponseDto user = userService.findByLoginId(principal.getName());
        
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setUserName(user.getUserName());
        updateDto.setPhone(user.getPhone());
        updateDto.setEmail(user.getEmail());
        updateDto.setAddress(user.getAddress());
        
        // 차량 정보 초기화
        updateDto.setVehicles(user.getVehicles().stream()
                .map(car -> {
                    VehicleUpdateDto v = new VehicleUpdateDto();
                    v.setVehicleId(car.getVehicleId());
                    v.setCarNumber(car.getCarNumber());
                    v.setSerialNumber(car.getSerialNumber());
                    return v;
                })
                .collect(Collectors.toList()));
        
        model.addAttribute("user", updateDto);
        return "users/mypage";
    }

    @PostMapping
    public String updateProfile(Principal principal, @ModelAttribute("user") UserUpdateDto updateDto) {
        userService.updateProfile(principal.getName(), updateDto);
        return "redirect:/mypage?success";
    }
}
