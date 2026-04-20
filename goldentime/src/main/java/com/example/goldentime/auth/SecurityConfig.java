package com.example.goldentime.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 기존 DB에 평문 비밀번호가 저장된 케이스(마이그레이션 전)까지 임시 호환
        // - 신규/변경 비밀번호는 BCrypt로 저장
        // - 저장된 값이 BCrypt 형식($2a/$2b/$2y) 이면 BCrypt로 검증
        // - 그 외는 평문 비교
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return bcrypt.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                if (encodedPassword == null) return false;
                String s = encodedPassword.trim();
                if (s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$")) {
                    return bcrypt.matches(rawPassword, s);
                }
                return rawPassword != null && rawPassword.toString().equals(s);
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                // 세션/쿠키 기반 로그인 사용 시 CSRF 보호 활성화
                // 프론트는 XSRF-TOKEN 쿠키를 읽어 X-XSRF-TOKEN 헤더로 전송
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // 로그인/회원가입/로그아웃은 CSRF 예외로 두어 초기 진입 시 막히지 않게 함
                        .ignoringRequestMatchers("/api/auth/**")
                        // 마이페이지 GET/PUT·차량 등록 POST 등 — JSON fetch 에서 CSRF 헤더 누락 시 403 방지 (인증은 아래 authorize 에서 유지)
                        .ignoringRequestMatchers("/api/users/me", "/api/users/me/**")
                        // multipart 공지 저장 시 브라우저/프록시 환경에서 CSRF 헤더가 누락되어 403이 나는 경우가 많음.
                        // 세션 인증 + hasRole("ADMIN") 은 그대로 적용됨.
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/api/notices", HttpMethod.POST.name()),
                                new AntPathRequestMatcher("/api/notices/**", HttpMethod.PUT.name()),
                                new AntPathRequestMatcher("/api/notices/**", HttpMethod.DELETE.name()),
                                // 신고 목록: 전송(POST) / 삭제(DELETE) — 세션 인증은 유지, CSRF 누락 403 방지
                                new AntPathRequestMatcher("/api/dashboard/events/**", HttpMethod.DELETE.name()),
                                new AntPathRequestMatcher("/api/dashboard/events/*/send", HttpMethod.POST.name()),
                                // 영상 업로드 multipart — CSRF 헤더/폼 누락 시 403 방지 (Principal 로그인 검사는 컨트롤러)
                                new AntPathRequestMatcher("/api/dashboard/upload", HttpMethod.POST.name()),
                                // 회원관리 권한 변경(관리자 전용)은 CSRF 누락 403이 자주 발생
                                new AntPathRequestMatcher("/api/users/*/role", HttpMethod.PUT.name()),
                                // 회원 삭제(관리자 전용)도 동일하게 CSRF 누락 403 방지
                                new AntPathRequestMatcher("/api/users/*", HttpMethod.DELETE.name())
                        )
                )
                // SPA에서 첫 요청부터 XSRF-TOKEN 쿠키가 세팅되도록 강제 로드
                .addFilterAfter(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                            throws ServletException, IOException {
                        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                        if (csrfToken != null) {
                            csrfToken.getToken();
                        }
                        filterChain.doFilter(request, response);
                    }
                }, CsrfFilter.class)
                // XSS/클릭재킹 등 기본 보안 헤더
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(String.join("; ",
                                // 기본은 self만 허용
                                "default-src 'self'",
                                // Vite 번들(정적 assets) 로드
                                "script-src 'self'",
                                // Google Fonts stylesheet는 외부 로드 필요
                                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
                                // Google Fonts font 파일
                                "font-src 'self' https://fonts.gstatic.com data:",
                                // 이미지/미리보기(data/blob) 허용
                                "img-src 'self' data: blob: https://*.supabase.co",
                                // Supabase Storage 영상 재생 허용
                                "media-src 'self' blob: https://*.supabase.co",
                                // API 호출은 동일 origin(배포 시) 기준
                                "connect-src 'self' https://*.supabase.co",
                                // 문의게시판: Google Forms iframe — default-src만 있으면 frame-src가 self로 떨어져 임베드 차단됨
                                "frame-src 'self' https://docs.google.com https://*.google.com https://*.gstatic.com https://*.googleusercontent.com",
                                // 이 사이트를 다른 origin에 iframe으로 넣는 것만 차단(클릭재킹 방지)
                                "frame-ancestors 'none'",
                                // 위험한 플러그인 차단
                                "object-src 'none'",
                                // base 태그 악용 방지
                                "base-uri 'self'"
                        )))
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN
                        ))
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        // 일부 브라우저/환경에서의 최소 방어(레거시). 최신 브라우저는 CSP가 핵심.
                        .xssProtection(xss -> xss.disable())
                )
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight는 항상 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/index.html", "/login", "/register", "/dashboard", "/notice", "/mypage", "/users", "/inquiry", "/main", "/error").permitAll()
                        .requestMatchers("/api/auth/**", "/api/ocr/**").permitAll()
                        // /api/users/me/** 만 있으면 정확히 "/api/users/me" 가 매칭되지 않아
                        // 다음 줄 /api/users/**(ADMIN 전용)에 걸려 일반 사용자가 403 발생함
                        .requestMatchers("/api/users/me", "/api/users/me/**").authenticated()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/notices", "/api/notices/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/notices").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/notices/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/notices/**").hasRole("ADMIN")
                        .requestMatchers("/assets/**", "/logos/**", "/css/**", "/js/**", "/images/**", "/uploads/**", "/videos/**", "/*.svg", "/*.ico", "/*.mp4").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(200);
                        })
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(401);
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);
                        })
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 모든 미인증 요청에 대해 401 Unauthorized 반환 (브라우저 팝업 방지)
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                )
                .build();
    }
}
