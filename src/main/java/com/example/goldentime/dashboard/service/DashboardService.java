package com.example.goldentime.dashboard.service;

import com.example.goldentime.dashboard.dto.GtEventResponseDto;
import com.example.goldentime.dashboard.dto.GtEventSaveRequestDto;
import com.example.goldentime.dashboard.entity.GtEvent;
import com.example.goldentime.dashboard.repository.GtEventRepository;
import com.example.goldentime.user.entity.UserVehicle;
import com.example.goldentime.user.repository.UserVehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final GtEventRepository gtEventRepository;
    private final UserVehicleRepository userVehicleRepository;

    public DashboardService(GtEventRepository gtEventRepository, UserVehicleRepository userVehicleRepository) {
        this.gtEventRepository = gtEventRepository;
        this.userVehicleRepository = userVehicleRepository;
    }

    @Transactional
    public GtEvent saveEvent(GtEventSaveRequestDto requestDto) throws IOException {
        UserVehicle vehicle = userVehicleRepository.findById(requestDto.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("차량을 찾을 수 없습니다. ID: " + requestDto.getVehicleId()));

        GtEvent event = new GtEvent();
        event.setVehicle(vehicle);
        event.setLatitude(requestDto.getLatitude());
        event.setLongitude(requestDto.getLongitude());
        event.setStatus(requestDto.getStatus());
        event.setOcrAttempted(requestDto.isOcrAttempted());
        event.setExposureDuration(requestDto.getExposureDuration());

        if (requestDto.getVideoFile() != null && !requestDto.getVideoFile().isEmpty()) {
            String videoPath = saveVideo(requestDto.getVideoFile());
            event.setVideoPath(videoPath);
        }

        return gtEventRepository.save(event);
    }

    private String saveVideo(MultipartFile file) throws IOException {
        String projectPath = System.getProperty("user.dir");
        String fullPath = projectPath + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "videos";
        
        File directory = new File(fullPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(fullPath, fileName);
        Files.write(filePath, file.getBytes());

        return "/videos/" + fileName;
    }

    @Transactional(readOnly = true)
    public List<GtEventResponseDto> findAllEvents() {
        return gtEventRepository.findAll().stream()
                .map(GtEventResponseDto::new)
                .collect(Collectors.toList());
    }
}
