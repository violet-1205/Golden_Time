package com.example.goldentime.notice.dto;

import com.example.goldentime.notice.entity.Notice;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class NoticeSaveRequestDto {
    private Long noticeId;
    private String title;
    private String content;
    private MultipartFile imageFile;

    public Notice toEntity() {
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setAdminId(1); // 임시 관리자 ID
        notice.setViewCount(0);
        return notice;
    }
}
