package com.example.goldentime.notice.controller;

import com.example.goldentime.notice.dto.NoticeSaveRequestDto;
import com.example.goldentime.notice.service.NoticeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Controller
@RequestMapping("/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/new")
    public String newNotice(Model model) {
        model.addAttribute("notice", new NoticeSaveRequestDto());
        return "notices/form";
    }

    @PostMapping
    public String saveNotice(@ModelAttribute("notice") NoticeSaveRequestDto requestDto) throws IOException {
        noticeService.save(requestDto);
        return "redirect:/notices";
    }

    @GetMapping("/{id}/edit")
    public String editNotice(@PathVariable Long id, Model model) {
        noticeService.findById(id).ifPresent(notice -> {
            NoticeSaveRequestDto dto = new NoticeSaveRequestDto();
            dto.setNoticeId(notice.getNoticeId());
            dto.setTitle(notice.getTitle());
            dto.setContent(notice.getContent());
            model.addAttribute("notice", dto);
        });
        return "notices/form";
    }

    @PostMapping("/{id}")
    public String updateNotice(@PathVariable Long id, @ModelAttribute("notice") NoticeSaveRequestDto requestDto) throws IOException {
        noticeService.update(id, requestDto);
        return "redirect:/notices/{id}";
    }

    @PostMapping("/{id}/delete")
    public String deleteNotice(@PathVariable Long id) {
        noticeService.delete(id);
        return "redirect:/notices";
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("notices", noticeService.findAll());
        return "notices/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        noticeService.findById(id).ifPresent(notice -> model.addAttribute("notice", notice));
        return "notices/detail";
    }
}
