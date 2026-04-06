# 골든타임 (Golden Time)

차량 주행 영상을 업로드하면 번호판 OCR 파이프라인으로 인식하고, 사건·통계·공지 등을 관리하는 웹 애플리케이션입니다.

## 구성

| 영역 | 경로 | 기술 |
|------|------|------|
| 프론트엔드 | `golden/golden/` | Vue 3, Vite, Pinia |
| 백엔드 API·정적 호스팅 | `goldentime/` | Spring Boot 3.2, Java 17, PostgreSQL |
| OCR 서버 | `num/` | FastAPI, PaddleOCR, YOLO(PlateEnginePro 등) |

프로덕션 배포 시 **Vue 빌드 산출물**을 `goldentime/src/main/resources/static/`에 두고 Spring Boot JAR로 함께 제공하는 방식을 사용합니다.

## 요구 사항

- **JDK 17**
- **Node.js** (프론트 빌드)
- **PostgreSQL** (백엔드 DB)
- **Python 3.9+** (OCR 서버, `num/requirements.txt` 참고)

## 로컬 실행 개요

1. **DB**  
   `goldentime/src/main/resources/application.properties`에 맞게 PostgreSQL을 준비합니다.

2. **백엔드** (`goldentime/`)

   ```bash
   ./gradlew bootRun
   ```

   기본 포트는 설정에 따라 다를 수 있습니다(예: `1111`).

3. **프론트 개발 서버** (`golden/golden/`, 선택)

   ```bash
   npm ci
   npm run dev
   ```

   Vite는 `/api`를 Spring으로 프록시하도록 설정되어 있습니다.

4. **OCR 서버** (`num/`)

   ```bash
   pip install -r requirements.txt
   python server.py
   ```

   Spring이 `http://localhost:8000/ocr`로 영상을 전달하는 구조입니다.

## 프론트 빌드 → 백엔드 반영

```bash
cd golden/golden
npm ci
npm run build
# dist/* 를 goldentime/src/main/resources/static/ 에 복사한 뒤 커밋
```

## CI

GitHub Actions(`.github/workflows/ci.yml`)에서 백엔드 Gradle 테스트·`bootJar`, `num/` 문법 검사(`compileall`)를 수행합니다. 프론트는 위와 같이 저장소에 빌드 결과를 포함하는 흐름을 전제로 합니다.

## 문서

- [OCR 신뢰도·평균 인식률](OCR_CONFIDENCE.md)
- [보안 관련](SECURITY_GUIDE.md)

## 라이선스

저장소 소유자·팀 정책에 따릅니다.
