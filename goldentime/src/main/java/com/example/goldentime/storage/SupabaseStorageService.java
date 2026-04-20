package com.example.goldentime.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Service
public class SupabaseStorageService {

    private final WebClient webClient;
    private final String supabaseUrl;
    private final String supabaseKey;
    private final String bucket;
    private final boolean enabled;

    public SupabaseStorageService(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.key:}") String supabaseKey,
            @Value("${supabase.bucket:goldentime-videos}") String bucket) {
        this.supabaseUrl = supabaseUrl;
        this.supabaseKey = supabaseKey;
        this.bucket = bucket;
        this.enabled = supabaseUrl != null && !supabaseUrl.isBlank();
        this.webClient = WebClient.create();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Supabase Storage에 파일 업로드 후 공개 URL 반환
     * Supabase 미설정 시 null 반환 (로컬 저장 fallback)
     */
    public String uploadVideo(byte[] fileBytes, String originalFileName, String contentType) {
        if (!enabled) return null;

        String fileName = UUID.randomUUID() + "_" + originalFileName;
        String uploadPath = "/storage/v1/object/" + bucket + "/" + fileName;

        webClient.post()
                .uri(supabaseUrl + uploadPath)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                .header("x-upsert", "true")
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "video/mp4"))
                .bodyValue(fileBytes)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + fileName;
    }

    public String getPublicUrl(String path) {
        if (!enabled || path == null) return path;
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    // Supabase Storage에서 파일 삭제 (public URL로부터 파일명 추출)
    public void deleteVideo(String publicUrl) {
        if (!enabled || publicUrl == null) return;
        try {
            String prefix = supabaseUrl + "/storage/v1/object/public/" + bucket + "/";
            if (!publicUrl.startsWith(prefix)) return;
            String fileName = publicUrl.substring(prefix.length());

            webClient.delete()
                    .uri(supabaseUrl + "/storage/v1/object/" + bucket + "/" + fileName)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Supabase 파일 삭제 실패: " + e.getMessage());
        }
    }
}
