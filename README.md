🌐 CueCode
수어·표정·제스처 기반 실시간 AI 멀티모달 커뮤니케이션 플랫폼
“말하지 못해도, 표현할 수 있어야 한다.”

Redesign Everything with AI (AI와 함께 만드는 새로운 변화)

🏷 기술 스택 배지










1. 프로젝트 개요
❗ 해결하려는 문제

대한민국 내 의사소통 취약계층(언어·청각·뇌병변 포함) 약 140만 명,
그리고 고령화로 인한 후천적 의사표현 장애 증가.

이들은 다음과 같은 어려움을 겪는다:

말을 하지 못함

손을 자유롭게 쓰기 어려움

UI 조작이 어려움

긴급 상황에서 도움 요청이 늦어짐

기존 AAC의 한계:

“터치 기반, 정적 문장, 복잡한 UI, 비언어 인식 불가”

→ 실제 장애·고령 사용자 환경과 맞지 않는 구조적 제약이 존재함.

2. CueCode가 제시하는 해결책

✔ 수어·표정·손동작 실시간 인식 → 문장 변환

✔ 사용자가 직접 만든 동작을 ‘문장’으로 학습하는 개인 맞춤형 커뮤니케이션 사전

✔ 응급·일상 문장을 AI가 자동 생성 + 음성 출력

✔ 복지·의료·요양 환경에서 바로 사용할 수 있는 즉시 적용형 커뮤니케이션 솔루션

3. 핵심 가치
🔵 디지털 접근성 확대

기존 클릭 기반 AAC의 한계를 넘어 비언어 인식 기반 차세대 커뮤니케이션 패러다임 제시.

🔵 AI가 대신 말해주는 시스템

멀티모달 정보를 기반으로 자연스러운 문장 자동 생성.

🔵 응급 상황 즉시 대응

수어·표정·제스처 하나로
“도와주세요” 같은 SOS 문장 자동 생성 및 음성 출력.

🔵 운영 가능한 엔드투엔드 구조

Azure 기반 컨테이너 + WebSocket + FastAPI로
실 서비스 수준의 안정성 확보.

4. 주요 기능
🖐 실시간 동작·표정 인식

MediaPipe FaceLandmarker / Hands 적용

468 Landmark / 52 Blendshape Features 추출

WebSocket 기반 100ms 단위 스트리밍

🔢 동작 매칭 (DTW 기반)

손좌표 정규화 (RMS Normalization)

Dynamic Time Warping 기반 템플릿 비교

사용자 맞춤형 동작 템플릿 학습

🤖 LangGraph 기반 LLM 오케스트레이션
모델	역할
Llama (Local)	Intent 분석, 욕설/이상 패턴 필터링, Fallback 문장 생성
GPT-4.1-mini	응급·일상 문장 생성, 존댓말 변환
Gemini	의미·안전성·톤·문장 길이 검증

→ 3중 AI 검증을 통한 안정적인 문장 생성 파이프라인 구축

🔊 문장·음성 출력

문장 카드 UI 출력

SpeechSynthesis API로 음성 피드백

5. 시스템 아키텍처
사용자 카메라 입력
 ↓
MediaPipe JS (Landmark + Blendshape)
 ↓ 100ms WebSocket
API Gateway (Spring Boot)
 ↓
Motion Service (DTW Matching)
 ↓
FastAPI (MediaPipe AI Engine)
 ↓
LangGraph (Llama + GPT + Gemini)
 ↓
최종 문장 출력 + 음성 피드백

6. 기술 스택
🔷 Frontend

JavaScript

WebSocket

MediaPipe JS

🔷 Backend

Spring Boot (MSA)

FastAPI (AI 분석 서버)

🔷 AI

Google MediaPipe

DTW Matching

LangGraph 기반 LLM Orchestration

🔷 Cloud

Azure Container Apps

GitHub Actions CI/CD

7. 서비스 3단계 확산 전략
① 시범 도입 (2026)

복지관·요양시설·재활병원 10곳 도입

사용자 피드백 기반 UI/UX 개선

② 전국 확산 (2027)

전국 20개 공공기관 도입

복지·의료 데이터 협력 체계 강화

SNS 기반 접근성 캠페인 확대

③ 고도화 및 글로벌 확장 (2027~)

다국어·글로벌 수어 모델 확장

NGO 협력 및 국제 공동 연구

의료·요양 전문 현장에 최적화된 기능 고도화

8. 향후 발전 계획

수어 문장 전체 인식 모델 개발

감정 기반 표정 인식

고령층 최적화 모드 제공

의료·케어 현장 특화 기능 확장

9. 프로젝트 철학

“의사소통은 권리이며 생존이다.”
CueCode는 말하기 어려운 사람에게
새로운 언어 시스템을 제공한다.
