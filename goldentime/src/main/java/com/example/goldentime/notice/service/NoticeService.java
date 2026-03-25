package com.example.goldentime.notice.service;

import com.example.goldentime.notice.dto.NoticeResponseDto;
import com.example.goldentime.notice.dto.NoticeSaveRequestDto;
import com.example.goldentime.notice.entity.Notice;
import com.example.goldentime.notice.repository.NoticeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    @Value("${app.upload.path}")
    private String uploadPath;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    @Transactional
    public Notice save(NoticeSaveRequestDto requestDto) throws IOException {
        Notice notice = requestDto.toEntity();
        if (requestDto.getImageFile() != null && !requestDto.getImageFile().isEmpty()) {
            String imagePath = saveFile(requestDto.getImageFile());
            notice.setImagePath(imagePath);
        }
        return noticeRepository.save(notice);
    }

    @Transactional
    public Notice update(Long id, NoticeSaveRequestDto requestDto) throws IOException {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid notice Id:" + id));
        notice.setTitle(requestDto.getTitle());
        notice.setContent(requestDto.getContent());
        notice.setImportant(Boolean.TRUE.equals(requestDto.getImportant()));

        if (requestDto.getImageFile() != null && !requestDto.getImageFile().isEmpty()) {
            String imagePath = saveFile(requestDto.getImageFile());
            notice.setImagePath(imagePath);
        }
        return noticeRepository.save(notice);
    }

    private String saveFile(MultipartFile file) throws IOException {
        String projectPath = System.getProperty("user.dir");
        String fullPath = projectPath + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "images";
        
        File directory = new File(fullPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(fullPath, fileName);
        Files.write(filePath, file.getBytes());

        return "/images/" + fileName;
    }

    @Transactional
    public void delete(Long id) {
        noticeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<NoticeResponseDto> findAll() {
        return noticeRepository.findAll().stream()
                .map(NoticeResponseDto::new)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public Optional<NoticeResponseDto> findById(Long id) {
        return noticeRepository.findById(id).map(notice -> {
            notice.setViewCount((notice.getViewCount() == null ? 0 : notice.getViewCount()) + 1);
            noticeRepository.save(notice);
            return new NoticeResponseDto(notice);
        });
    }
}
