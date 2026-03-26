package com.example.goldentime.api.dashboard;

import com.example.goldentime.car.dto.CarResponseDto;
import com.example.goldentime.dashboard.dto.DashboardStatsDto;
import com.example.goldentime.dashboard.dto.GtEventResponseDto;
import com.example.goldentime.dashboard.dto.GtEventSaveRequestDto;
import com.example.goldentime.dashboard.service.DashboardService;
import com.example.goldentime.user.repository.UserVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardService dashboardService;
    private final UserVehicleRepository userVehicleRepository;

    @GetMapping("/events")
    public ResponseEntity<List<GtEventResponseDto>> getAllEvents() {
        return ResponseEntity.ok(dashboardService.findAllEvents());
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    @GetMapping("/recent-events")
    public ResponseEntity<List<GtEventResponseDto>> getRecentEvents(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        // 관리자 대시보드(/main)는 전체 신고 현황이 필요하고, 관리자 계정은 등록 차량이 없을 수 있음
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return ResponseEntity.ok(dashboardService.findTop5RecentEvents());
        }
        return ResponseEntity.ok(dashboardService.findTop5RecentEventsForUser(authentication.getName()));
    }

    @GetMapping("/events-by-region")
    public ResponseEntity<Map<String, Long>> getEventsByRegion() {
        return ResponseEntity.ok(dashboardService.getEventCountsByRegion());
    }

    @GetMapping("/cars")
    public ResponseEntity<List<CarResponseDto>> getAllCars() {
        List<CarResponseDto> cars = userVehicleRepository.findAll().stream()
                .map(CarResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(cars);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadEvent(Principal principal, @ModelAttribute GtEventSaveRequestDto requestDto) throws IOException {
        if (principal == null) {
            System.err.println("Upload failed: Principal is null (User not logged in)");
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        System.out.println("Upload request received from user: " + principal.getName());
        try {
            dashboardService.saveEvent(principal.getName(), requestDto);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Upload failed for user " + principal.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/events/{gtId}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long gtId) {
        System.out.println("Delete request received for event ID: " + gtId);
        try {
            dashboardService.deleteEvent(gtId);
            System.out.println("Successfully deleted event ID: " + gtId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Delete failed for event ID " + gtId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/events/{gtId}/send")
    public ResponseEntity<?> sendEvent(@PathVariable Long gtId, @RequestParam String target) {
        dashboardService.sendEvent(gtId, target);
        return ResponseEntity.ok().build();
    }
}
