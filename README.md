🌐 CueCode
수어·표정·제스처 기반 실시간 AI 멀티모달 커뮤니케이션 플랫폼
“말하지 못해도, 표현할 수 있어야 한다.”

Redesign Everything with AI (AI와 함께 만드는 새로운 변화)

<p align="center"> <img src="https://img.shields.io/badge/AI-Multimodal-blue?style=flat-square" /> <img src="https://img.shields.io/badge/MSA-SpringBoot-green?style=flat-square" /> <img src="https://img.shields.io/badge/LLM-Orchestration-orange?style=flat-square" /> <img src="https://img.shields.io/badge/Backend-FastAPI-yellow?style=flat-square" /> <img src="https://img.shields.io/badge/Cloud-Azure-0078D4?style=flat-square" /> </p>
1. 프로젝트 개요
❗ 해결하려는 문제

대한민국 내 의사소통 취약계층(언어·청각·뇌병변 장애 포함) 약 140만 명,
그리고 고령화로 인한 후천적 의사표현 어려움 증가.

이들은 다음과 같은 어려움을 겪는다:

말을 하지 못한다

손을 자유롭게 쓰기 어렵다

UI 조작이 힘들다

긴급 상황에서 도움 요청이 늦어진다

기존 AAC의 한계:

“터치 기반, 정적 문장, 복잡한 UI, 비언어 인식 불가”

→ 실제 장애·고령 사용자 환경과 맞지 않는 구조적 한계가 존재함.

2. CueCode가 제시하는 해결책
✔ 수어·표정·손동작을 실시간으로 인식하여 언어로 변환
✔ 사용자가 직접 만든 동작을 ‘문장’으로 학습하는 개인 맞춤형 커뮤니케이션 사전
✔ 응급·일상 문장을 AI가 자동 생성 및 음성 출력
✔ 복지·의료·요양 등 현장에서 즉시 활용 가능한 실시간 커뮤니케이션 솔루션
3. 핵심 가치
🔵 디지털 접근성 확대

클릭 중심 AAC의 한계를 넘어,
비언어적 표현 기반의 차세대 소통 패러다임을 제시한다.

🔵 AI가 대신 말해주는 시스템

멀티모달 AI가 동작·표정을 자연스러운 문장으로 자동 변환한다.

🔵 응급 상황 즉시 대응

손짓·표정 하나로
“도와주세요” 같은 SOS 문장 자동 생성 및 음성 출력.

🔵 운영 가능한 엔드투엔드 구조

Azure 기반 MSA + WebSocket + FastAPI로
실제 서비스 운영 가능한 안정성 확보.

4. 주요 기능
🖐 실시간 동작·표정 인식

MediaPipe FaceLandmarker / Hands

Landmark + Blendshape 468/52 vector 추출

100ms 간격 WebSocket 실시간 스트리밍

🔢 동작 매칭 (DTW 기반)

손동작 좌표 정규화 (RMS Normalization)

Dynamic Time Warping으로 템플릿과 비교

사용자 커스텀 동작 학습 가능

🤖 LangGraph 기반 LLM 오케스트레이션
모델	역할
Llama (Local)	Intent 분류 / 욕설·이상패턴 필터링 / Fallback 생성
GPT (4.1-mini)	응급/일상 문장 생성 / 존댓말 스타일 변환
Gemini	의미 안전성·품질·길이 검증 / Tone Checker

→ 3중 AI 검증을 통한 고신뢰 커뮤니케이션 생성 파이프라인

🔊 문장·음성 출력

Front UI에서 문장 카드로 출력

SpeechSynthesis 음성 재생 지원

5. 아키텍처 개요
User Camera
 ↓
MediaPipe JS (Real-Time Landmark)
 ↓ WebSocket (100ms)
API Gateway (Spring Boot)
 ↓
Motion Service (DTW Matching)
 ↓
FastAPI (MediaPipe AI Engine)
 ↓
LangGraph AI Pipeline
 - Llama Intent
 - GPT Sentence Generator
 - Gemini Safety Checker
 ↓
Final Output (Text + Voice)

6. 기술 스택
🔷 Frontend

JavaScript

WebSocket Streaming

MediaPipe JS

🔷 Backend

Spring Boot (MSA)

FastAPI (AI Server)

🔷 AI

MediaPipe FaceLandmarker/Hands

DTW Matching

LangGraph Multi-LLM Orchestration

🔷 Cloud

Azure Container Apps

GitHub Actions CI/CD

7. 서비스 영향력 (3단계 로드맵)
① 시범 도입 단계 (2026)

복지관·요양원 등 10개 기관 시범 도입

사용자 피드백 기반 UI/UX 고도화

② 전국 확산 단계 (2027)

전국 20개 공공기관 도입

보건복지부·서울시 복지재단과 데이터 협력

SNS 기반 디지털 홍보 확산

③ 고도화 및 글로벌 확장 단계 (2027~)

다국어·수어 모델 확장

글로벌 NGO 협력

지속 가능 운영 모델 구축

8. 향후 발전 계획

전체 수어 문장 인식 모델 개발

감정 기반 표정 인식 추가

고령층 최적화 UI 개발

의료·케어 환경 특화 기능 추가

9. 프로젝트 철학

“의사소통은 권리이며 생존이다.”
CueCode는 말하기 어려운 사람에게
새로운 언어 시스템을 제공하는 플랫폼이다.
