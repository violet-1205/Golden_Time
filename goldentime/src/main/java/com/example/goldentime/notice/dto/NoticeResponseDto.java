package com.example.goldentime.notice.dto;

import com.example.goldentime.notice.entity.Notice;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class NoticeResponseDto {
    private Long id;
    private String title;
    private String content;
    private boolean important;
    private String author;
    private String image;
    private Integer views;
    private LocalDateTime createdAt;

    public NoticeResponseDto(Notice entity) {
        this.id = entity.getNoticeId();
        this.title = entity.getTitle();
        this.content = entity.getContent();
        this.important = entity.isImportant();
        this.author = "관리자";
        this.image = entity.getImagePath();
        this.views = entity.getViewCount();
        this.createdAt = entity.getCreatedAt();
    }
}
