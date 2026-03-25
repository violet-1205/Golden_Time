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
    private Boolean important;
    private MultipartFile imageFile;

    public Notice toEntity() {
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setViewCount(0);
        notice.setImportant(Boolean.TRUE.equals(important));
        return notice;
    }

    public Long getNoticeId() { return noticeId; }
    public void setNoticeId(Long noticeId) { this.noticeId = noticeId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public MultipartFile getImageFile() { return imageFile; }
    public void setImageFile(MultipartFile imageFile) { this.imageFile = imageFile; }
}
