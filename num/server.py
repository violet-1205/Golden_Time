import os
import tempfile
import time
import sys

# oneDNN 충돌 해결 및 권한 문제 우회를 위한 환경 변수 설정
os.environ['KMP_DUPLICATE_LIB_OK']='True'
os.environ['FLAGS_use_mkldnn_kernel']='0'
os.environ['FLAGS_use_mkldnn']='0'
os.environ['FLAGS_enable_mkldnn']='0'

# 모델 다운로드 경로를 프로젝트 내부로 변경 (권한 문제 해결)
project_num_path = os.path.dirname(os.path.abspath(__file__))
os.environ['HOME'] = project_num_path
os.environ['USERPROFILE'] = project_num_path

# 로컬 num/ocr.py를 확실히 import (pip 패키지명 충돌 방지)
if project_num_path not in sys.path:
    sys.path.insert(0, project_num_path)

from fastapi import FastAPI, UploadFile, File
import uvicorn
from ocr import recognize_plate_from_video

app = FastAPI()

@app.post("/ocr")
async def ocr_endpoint(file: UploadFile = File(...)):
    start_time = time.time()
    print(f"\n[OCR Request] Received file: {file.filename}")
    try:
        # 임시 파일로 저장
        with tempfile.NamedTemporaryFile(delete=False, suffix=os.path.splitext(file.filename)[1]) as temp_video:
            temp_video.write(await file.read())
            temp_video_path = temp_video.name
        
        print(f"[OCR Request] Saved to temporary path: {temp_video_path}")
        
        # OCR 처리
        result = recognize_plate_from_video(temp_video_path)
        
        # 터미널에 결과 출력
        if "error" in result:
            print(f"[OCR Error] {result['error']}")
        else:
            plate = result.get("detected_plate", "N/A")
            confidence = result.get("confidence", 0.0)
            print(f"[OCR Success] Plate: {plate}, Confidence: {confidence:.2f}")

        # 임시 파일 삭제
        os.unlink(temp_video_path)
        
        elapsed_time = time.time() - start_time
        print(f"[OCR Request] Completed in {elapsed_time:.2f}s")
        return result

    except Exception as e:
        error_message = f"[Server Error] {str(e)}"
        print(error_message)
        return {"error": error_message}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
