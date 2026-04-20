# 배포 가이드 (Vercel + Railway + Supabase + Neon)

## 구조

```
[Vercel]          [Railway - Backend]     [Railway - OCR]
Vue 3 Frontend → Spring Boot API    ←→   FastAPI OCR Server
                       ↕
                 [Neon PostgreSQL]
                 [Supabase Storage]
```

---

## 1. Neon (PostgreSQL) 설정

1. https://neon.tech 에서 프로젝트 생성
2. Connection string 복사 (형식: `postgresql://user:pass@host/db?sslmode=require`)
3. Railway 백엔드 환경변수에 추가:
   - `DATABASE_URL` = 복사한 connection string

---

## 2. Supabase Storage 설정

1. https://supabase.com 에서 프로젝트 생성
2. Storage → New Bucket → 이름: `goldentime-videos`, Public 체크
3. Settings → API → `Project URL`, `service_role` key 복사
4. Railway 백엔드 환경변수에 추가:
   - `SUPABASE_URL` = Project URL
   - `SUPABASE_SERVICE_KEY` = service_role key
   - `SUPABASE_BUCKET` = goldentime-videos

---

## 3. Railway - Spring Boot 백엔드 배포

1. https://railway.app → New Project → GitHub repo 연결
2. `goldentime/` 폴더를 Root Directory로 설정
3. 환경변수 설정:

```
DATABASE_URL=postgresql://...  (Neon connection string)
DB_USERNAME=                   (Neon에서는 DATABASE_URL에 포함, 미사용)
DB_PASSWORD=                   (Neon에서는 DATABASE_URL에 포함, 미사용)
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_SERVICE_KEY=eyJ...
SUPABASE_BUCKET=goldentime-videos
OCR_SERVICE_URL=https://your-ocr.railway.app  (OCR 배포 후 입력)
CORS_ALLOWED_ORIGINS=http://localhost:5173,https://your-app.vercel.app
COOKIE_SECURE=true
PORT=8080
```

4. 배포 후 URL 메모: `https://your-backend.railway.app`

---

## 4. Railway - OCR 서버 배포

1. Railway → New Service → GitHub repo 연결
2. `num/` 폴더를 Root Directory로 설정
3. 환경변수: 특별한 설정 없음 (PORT는 Railway 자동 제공)
4. 배포 후 URL → 백엔드의 `OCR_SERVICE_URL`에 입력

> ⚠️ OCR 서버는 PaddleOCR + YOLO 모델 크기로 인해 Railway 무료 플랜 메모리(512MB)를 초과할 수 있습니다.
> 초과 시 Hugging Face Spaces (CPU 무료)로 대체하거나 Railway 유료 플랜 사용.

---

## 5. Vercel - 프론트엔드 배포

1. https://vercel.com → New Project → GitHub repo 연결
2. Root Directory: `golden/golden`
3. Framework: Vite
4. 환경변수 설정:
   - `VITE_API_BASE_URL` = `https://your-backend.railway.app`
5. Deploy

---

## 6. CORS 최종 업데이트

Vercel 배포 후 생성된 URL(`https://xxx.vercel.app`)을 Railway 백엔드의
`CORS_ALLOWED_ORIGINS` 환경변수에 추가:

```
CORS_ALLOWED_ORIGINS=https://xxx.vercel.app,https://골든타임.net,...
```

---

## 개발 환경 (로컬)

환경변수 없이 그대로 실행하면 기존 로컬 설정 사용:
- DB: localhost:5432
- 파일: external-data/videos/ (로컬 저장)
- OCR: localhost:8000
