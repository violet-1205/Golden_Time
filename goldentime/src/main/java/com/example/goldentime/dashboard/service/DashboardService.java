package com.example.goldentime.dashboard.service;

import com.example.goldentime.dashboard.dto.DashboardStatsDto;
import com.example.goldentime.dashboard.dto.GtEventResponseDto;
import com.example.goldentime.dashboard.dto.GtEventSaveRequestDto;
import com.example.goldentime.dashboard.entity.GtEvent;
import com.example.goldentime.dashboard.entity.GtOcr;
import com.example.goldentime.dashboard.repository.GtEventRepository;
import com.example.goldentime.dashboard.repository.GtOcrRepository;
import com.example.goldentime.storage.SupabaseStorageService;
import com.example.goldentime.user.entity.User;
import com.example.goldentime.user.entity.UserVehicle;
import com.example.goldentime.user.repository.UserRepository;
import com.example.goldentime.user.repository.UserVehicleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.time.ZoneId;

@Service
public class DashboardService {

    private final GtEventRepository gtEventRepository;
    private final UserVehicleRepository userVehicleRepository;
    private final UserRepository userRepository;
    private final GtOcrRepository gtOcrRepository;
    private final WebClient webClient;
    private final SupabaseStorageService supabaseStorageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String[] RANDOM_LOCATIONS = {
        "서울특별시 강남구 테헤란로 123", "부산광역시 해운대구 우동 456", "대구광역시 중구 동성로 789",
        "인천광역시 연수구 송도동 101", "광주광역시 서구 치평동 202", "대전광역시 유성구 봉명동 303",
        "울산광역시 남구 삼산동 404", "세종특별자치시 나성동 505", "경기도 수원시 팔달구 인계동 606",
        "강원도 춘천시 퇴계동 707", "충청북도 청주시 상당구 808", "충청남도 천안시 서북구 909",
        "전라북도 전주시 완산구 111", "전라남도 목포시 상동 222", "경상북도 포항시 남구 333",
        "경상남도 창원시 의창구 444", "제주특별자치도 제주시 노형동 555"
    };

    private final OcrPersistenceService ocrPersistenceService;

    public DashboardService(GtEventRepository gtEventRepository,
                            UserVehicleRepository userVehicleRepository,
                            UserRepository userRepository,
                            GtOcrRepository gtOcrRepository,
                            OcrPersistenceService ocrPersistenceService,
                            SupabaseStorageService supabaseStorageService,
                            @Value("${app.ocr.url:http://localhost:8000}") String ocrUrl) {
        this.gtEventRepository = gtEventRepository;
        this.userVehicleRepository = userVehicleRepository;
        this.userRepository = userRepository;
        this.gtOcrRepository = gtOcrRepository;
        this.ocrPersistenceService = ocrPersistenceService;
        this.supabaseStorageService = supabaseStorageService;
        this.webClient = WebClient.create(ocrUrl);
    }

    @Transactional
    public GtEvent saveEvent(String loginId, GtEventSaveRequestDto requestDto) throws IOException {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + loginId));

        UserVehicle vehicle;
        if (requestDto.getVehicleId() != null) {
            vehicle = userVehicleRepository.findById(requestDto.getVehicleId())
                    .orElseThrow(() -> new IllegalArgumentException("차량을 찾을 수 없습니다. ID: " + requestDto.getVehicleId()));
        } else {
            List<UserVehicle> vehicles = userVehicleRepository.findAllByUserUserId(user.getUserId());
            if (vehicles.isEmpty()) {
                throw new IllegalStateException("등록된 차량이 없어 영상을 업로드할 수 없습니다. 마이페이지에서 차량을 먼저 등록해주세요.");
            }
            vehicle = vehicles.get(0);
        }

        GtEvent event = new GtEvent();
        event.setVehicle(vehicle);

        if (requestDto.getVtIdPath() == null || requestDto.getVtIdPath().trim().isEmpty()) {
            String randomLoc = RANDOM_LOCATIONS[new Random().nextInt(RANDOM_LOCATIONS.length)];
            event.setVtIdPath(randomLoc);
        } else {
            event.setVtIdPath(requestDto.getVtIdPath());
        }

        GtEvent savedEvent = gtEventRepository.save(event);

        if (requestDto.getVideoFile() != null && !requestDto.getVideoFile().isEmpty()) {
            MultipartFile file = requestDto.getVideoFile();

            if (supabaseStorageService.isEnabled()) {
                // Supabase Storage에 업로드
                String publicUrl = supabaseStorageService.uploadVideo(
                        file.getBytes(),
                        file.getOriginalFilename(),
                        file.getContentType()
                );
                savedEvent.setVideoPath(publicUrl);
                gtEventRepository.save(savedEvent);
                runOcrAndSaveResultFromLocalTemp(savedEvent, file);
            } else {
                // 로컬 저장 (개발 환경)
                Path videoPath = saveVideoLocally(file);
                savedEvent.setVideoPath("/videos/" + videoPath.getFileName().toString());
                gtEventRepository.save(savedEvent);
                runOcrAndSaveResult(savedEvent, videoPath);
            }
        }

        return savedEvent;
    }

    private void runOcrAndSaveResult(GtEvent event, Path videoPath) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(videoPath));

        webClient.post()
                .uri("/ocr")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(Map.class)
                .subscribe(result -> {
                    if (result.containsKey("error")) {
                        System.err.println("OCR API error: " + result.get("error"));
                        return;
                    }
                    ocrPersistenceService.saveOcrResult(event.getGtId(), result);
                }, error -> {
                    System.err.println("OCR request failed for event " + event.getGtId() + ": " + error.getMessage());
                });
    }

    // Supabase 사용 시: 임시파일로 저장 후 OCR 전송 (OCR 서버도 Railway에 있으면 URL 방식으로 변경 가능)
    private void runOcrAndSaveResultFromLocalTemp(GtEvent event, MultipartFile file) {
        try {
            Path tempPath = Files.createTempFile("ocr_", "_" + file.getOriginalFilename());
            Files.write(tempPath, file.getBytes());
            runOcrAndSaveResult(event, tempPath);
            // 임시 파일은 JVM 종료 시 자동 삭제 예약
            tempPath.toFile().deleteOnExit();
        } catch (IOException e) {
            System.err.println("Failed to create temp file for OCR: " + e.getMessage());
        }
    }

    private Path saveVideoLocally(MultipartFile file) throws IOException {
        String projectPath = System.getProperty("user.dir");
        String fullPath = projectPath + File.separator + "external-data" + File.separator + "videos";

        File directory = new File(fullPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(fullPath, fileName);
        Files.write(filePath, file.getBytes());
        return filePath;
    }

    @Transactional(readOnly = true)
    public List<GtEventResponseDto> findTop5RecentEvents() {
        return gtEventRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(GtEventResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GtEventResponseDto> findTop5RecentEventsForUser(String loginId) {
        if (loginId == null || loginId.isBlank()) return List.of();

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + loginId));

        List<UserVehicle> vehicles = userVehicleRepository.findAllByUserUserId(user.getUserId());
        if (vehicles.isEmpty()) return List.of();

        return gtEventRepository.findTop5ByVehicle_User_UserIdOrderByCreatedAtDesc(user.getUserId()).stream()
                .map(GtEventResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getEventCountsByRegion() {
        List<String> paths = gtEventRepository.findAllVtIdPaths();
        return paths.stream()
                .map(path -> path.split(" ")[0])
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    @Transactional(readOnly = true)
    public List<GtEventResponseDto> findAllEvents() {
        return gtEventRepository.findAll().stream()
                .map(GtEventResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GtEventResponseDto> findAllEventsForUser(String loginId) {
        if (loginId == null || loginId.isBlank()) return List.of();

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + loginId));

        List<UserVehicle> vehicles = userVehicleRepository.findAllByUserUserId(user.getUserId());
        if (vehicles.isEmpty()) return List.of();

        return gtEventRepository.findAllByVehicle_User_UserIdOrderByCreatedAtDesc(user.getUserId()).stream()
                .map(GtEventResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        java.time.LocalDateTime startOfDay = java.time.LocalDate.now(ZoneId.of("Asia/Seoul")).atStartOfDay();

        double avgConfidence = 0.0;
        if (gtEventRepository.count() > 0) {
            Double avg = gtOcrRepository.getAverageConfidence();
            if (avg != null) avgConfidence = avg;
        }
        long totalToday = gtEventRepository.countByCreatedAtAfter(startOfDay);
        long sentToFire = gtEventRepository.countByCreatedAtAfterAndSentToFireTrue(startOfDay);
        long sentToSafety = gtEventRepository.countByCreatedAtAfterAndSentToSafetyTrue(startOfDay);

        return new DashboardStatsDto(avgConfidence, totalToday, sentToFire, sentToSafety);
    }

    @Transactional
    public void deleteEvent(Long gtId) {
        GtEvent event = gtEventRepository.findById(gtId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다. ID: " + gtId));

        if (event.getVideoPath() != null) {
            if (supabaseStorageService.isEnabled()) {
                supabaseStorageService.deleteVideo(event.getVideoPath());
            } else {
                deleteVideoFileLocally(event.getVideoPath());
            }
        }

        gtEventRepository.delete(event);
    }

    @Transactional
    public void sendEvent(Long gtId, String target) {
        GtEvent event = gtEventRepository.findById(gtId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다. ID: " + gtId));

        if ("fire".equalsIgnoreCase(target)) {
            event.setSentToFire(true);
        } else if ("safety".equalsIgnoreCase(target)) {
            event.setSentToSafety(true);
        } else {
            throw new IllegalArgumentException("잘못된 전송 대상입니다: " + target);
        }

        gtEventRepository.save(event);
    }

    private void deleteVideoFileLocally(String videoPath) {
        try {
            String projectPath = System.getProperty("user.dir");
            Path filePath = Paths.get(projectPath, "external-data", "videos", videoPath.replace("/videos/", ""));
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Failed to delete video file: " + videoPath);
        }
    }
}
