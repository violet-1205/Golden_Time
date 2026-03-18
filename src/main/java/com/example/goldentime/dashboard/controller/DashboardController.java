package com.example.goldentime.dashboard.controller;

import com.example.goldentime.car.dto.CarResponseDto;
import com.example.goldentime.dashboard.dto.GtEventSaveRequestDto;
import com.example.goldentime.dashboard.service.DashboardService;
import com.example.goldentime.user.repository.UserVehicleRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserVehicleRepository userVehicleRepository;

    public DashboardController(DashboardService dashboardService, UserVehicleRepository userVehicleRepository) {
        this.dashboardService = dashboardService;
        this.userVehicleRepository = userVehicleRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("events", dashboardService.findAllEvents());
        model.addAttribute("eventDto", new GtEventSaveRequestDto());
        
        model.addAttribute("cars", userVehicleRepository.findAll().stream()
                .map(CarResponseDto::new)
                .collect(Collectors.toList()));
                
        return "dashboard/list";
    }

    @PostMapping("/upload")
    public String uploadEvent(@ModelAttribute GtEventSaveRequestDto requestDto) throws IOException {
        dashboardService.saveEvent(requestDto);
        return "redirect:/dashboard";
    }
}
