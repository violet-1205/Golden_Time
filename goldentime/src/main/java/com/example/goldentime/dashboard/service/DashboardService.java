package com.example.goldentime.dashboard.service;

import com.example.goldentime.dashboard.dto.DashboardStatsDto;
import com.example.goldentime.dashboard.dto.GtEventResponseDto;
import com.example.goldentime.dashboard.dto.GtEventSaveRequestDto;
import com.example.goldentime.dashboard.entity.GtEvent;
import com.example.goldentime.dashboard.entity.GtOcr;
import com.example.goldentime.dashboard.repository.GtEventRepository;
import com.example.goldentime.dashboard.repository.GtOcrRepository;
import com.example.goldentime.user.entity.User;
import com.example.goldentime.user.entity.UserVehicle;
import com.example.goldentime.user.repository.UserRepository;
import com.example.goldentime.user.repository.UserVehicleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.function.Function;

@Service
public class DashboardService {

    private final GtEventRepository gtEventRepository;
    private final UserVehicleRepository userVehicleRepository;
    private final UserRepository userRepository;
    private final GtOcrRepository gtOcrRepository;
    private final WebClient webClient;
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
                            OcrPersistenceService ocrPersistenceService) {
        this.gtEventRepository = gtEventRepository;
        this.userVehicleRepository = userVehicleRepository;
        this.userRepository = userRepository;
        this.gtOcrRepository = gtOcrRepository;
        this.ocrPersistenceService = ocrPersistenceService;
        this.webClient = WebClient.create("http://localhost:8000");
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
            // 차량이 지정되지 않은 경우 사용자의 첫 번째 차량 사용
            List<UserVehicle> vehicles = userVehicleRepository.findAllByUserUserId(user.getUserId());
            if (vehicles.isEmpty()) {
                throw new IllegalStateException("등록된 차량이 없어 영상을 업로드할 수 없습니다. 마이페이지에서 차량을 먼저 등록해주세요.");
            }
            vehicle = vehicles.get(0);
        }

        GtEvent event = new GtEvent();
        event.setVehicle(vehicle);
        
        // vt_id_path 설정 (입력값이 없으면 랜덤 위치 할당)
        if (requestDto.getVtIdPath() == null || requestDto.getVtIdPath().trim().isEmpty()) {
            String randomLoc = RANDOM_LOCATIONS[new Random().nextInt(RANDOM_LOCATIONS.length)];
            event.setVtIdPath(randomLoc);
        } else {
            event.setVtIdPath(requestDto.getVtIdPath());
        }

        GtEvent savedEvent = gtEventRepository.save(event);
        System.out.println("Saved initial GtEvent with ID: " + savedEvent.getGtId());

        if (requestDto.getVideoFile() != null && !requestDto.getVideoFile().isEmpty()) {
            Path videoPath = saveVideo(requestDto.getVideoFile());
            System.out.println("Video saved to: " + videoPath);
            savedEvent.setVideoPath("/videos/" + videoPath.getFileName().toString());
            gtEventRepository.save(savedEvent);
            System.out.println("Updated GtEvent with video path");

            // OCR 처리 (비동기)
            runOcrAndSaveResult(savedEvent, videoPath);
        }

        return savedEvent;
    }

    private void runOcrAndSaveResult(GtEvent event, Path videoPath) {
        System.out.println("Starting OCR request for event ID: " + event.getGtId() + ", video: " + videoPath);
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(videoPath));

        webClient.post()
                .uri("/ocr")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(Map.class)
                .subscribe(result -> {
                    System.out.println("Received OCR response for event " + event.getGtId() + ": " + result);
                    if (result.containsKey("error")) {
                        System.err.println("OCR API error: " + result.get("error"));
                        return;
                    }
                    // 별도 트랜잭션으로 OCR 결과 저장
                    ocrPersistenceService.saveOcrResult(event.getGtId(), result);
                }, error -> {
                    System.err.println("OCR request failed for event " + event.getGtId() + ": " + error.getMessage());
                    error.printStackTrace();
                });
    }

    private Path saveVideo(MultipartFile file) throws IOException {
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
        if (loginId == null || loginId.isBlank()) {
            return List.of();
        }

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + loginId));

        // 사용자가 등록한 차량이 없으면 최근 신고도 보여줄 수 없음
        List<UserVehicle> vehicles = userVehicleRepository.findAllByUserUserId(user.getUserId());
        if (vehicles.isEmpty()) {
            return List.of();
        }

        return gtEventRepository.findTop5ByVehicle_User_UserIdOrderByCreatedAtDesc(user.getUserId()).stream()
                .map(GtEventResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getEventCountsByRegion() {
        List<String> paths = gtEventRepository.findAllVtIdPaths();
        return paths.stream()
                .map(path -> path.split(" ")[0]) // 주소에서 첫 단어(지역명) 추출
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    @Transactional(readOnly = true)
    public List<GtEventResponseDto> findAllEvents() {
        return gtEventRepository.findAll().stream()
                .map(GtEventResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        Double avgConfidence = gtOcrRepository.getAverageConfidence();
        
        java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        long totalToday = gtEventRepository.countByCreatedAtAfter(startOfDay);
        long sentToFire = gtEventRepository.countByCreatedAtAfterAndSentToFireTrue(startOfDay);
        long sentToSafety = gtEventRepository.countByCreatedAtAfterAndSentToSafetyTrue(startOfDay);

        return new DashboardStatsDto(
            avgConfidence != null ? avgConfidence : 0.0,
            totalToday,
            sentToFire,
            sentToSafety
        );
    }

    @Transactional
    public void deleteEvent(Long gtId) {
        GtEvent event = gtEventRepository.findById(gtId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다. ID: " + gtId));
        
        // 연관된 OCR 데이터는 CascadeType.ALL에 의해 자동 삭제됨

        // 1. 비디오 파일 삭제 (옵션)
        if (event.getVideoPath() != null) {
            deleteVideoFile(event.getVideoPath());
        }
        
        // 3. 이벤트 삭제
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

    private void deleteVideoFile(String videoPath) {
        try {
            String projectPath = System.getProperty("user.dir");
            // videoPath는 "/videos/filename.mp4" 형식이므로 앞의 /를 제거하고 결합
            Path filePath = Paths.get(projectPath, "external-data", "videos", videoPath.replace("/videos/", ""));
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // 파일 삭제 실패는 로깅만 하고 계속 진행
            System.err.println("Failed to delete video file: " + videoPath);
        }
    }
}
