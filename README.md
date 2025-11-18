🌐 CueCode – 수어·표정·제스처 기반 실시간 AI 멀티모달 커뮤니케이션 플랫폼
AI와 함께 만드는 새로운 변화 (Redesign Everything with AI)
“말하지 못해도, 표현할 수 있어야 한다.”
<p align="center"> <img src="https://img.shields.io/badge/AI-Multimodal-blue?style=flat-square"> <img src="https://img.shields.io/badge/MSA-SpringBoot-green?style=flat-square"> <img src="https://img.shields.io/badge/AI-LLM%20Orchestration-orange?style=flat-square"> <img src="https://img.shields.io/badge/Framework-FastAPI-yellow?style=flat-square"> <img src="https://img.shields.io/badge/Cloud-Azure-blue?style=flat-square"> </p>
🏆 프로젝트 소개
❗ 해결하려는 문제

대한민국 내 약 140만명 이상의 언어·청각·뇌병변 장애인,
그리고 고령화로 인해 매년 증가하는 후천적 의사소통 취약계층(65세 이상 장애 55% 이상).

이들은

말을 하지 못하거나

손을 사용하기 어렵거나

UI 조작을 힘들어하며

긴급 상황에서 의사 표현의 시간이 늦어져 생명 안전까지 위험해지는
구조적 문제를 겪는다.

기존 AAC(보완·대체 의사소통) 시스템은
✔ 터치/클릭 기반
✔ 정적인 문장 템플릿
✔ 복잡한 UI
✔ 비언어적 신호(표정/제스처) 인식 불가
라는 한계를 갖는다.

🔥 CueCode는 AI가 인간의 비언어적 표현을 "언어로 번역"하는 새로운 소통 방식을 제안한다.

💡 CueCode가 제시하는 혁신
✔ 수어·표정·손동작을 실시간으로 인식
✔ 사용자의 개인 동작을 “문장”으로 학습
✔ 응급/일상 상황에 맞는 문장을 AI가 즉시 생성
✔ 음성·텍스트로 출력하여 누구나 바로 의사 표현 가능
✔ 의료·돌봄·노인 복지 현장에서 바로 적용 가능
🌈 핵심 가치 (Why CueCode?)
🔵 1. 디지털 접근성 혁신

“말하기-쓰기-터치” 중심의 기존 소통 방식을
동작 기반(Multimodal) 으로 재설계.

🔵 2. AI가 사용자를 대신 말한다

MediaPipe + DTW + LangGraph + GPT 조합으로
비언어 → 자연어 변환 자동화.

🔵 3. 사회적 약자를 위한 기술

장애·고령·응급 상황 사용자에게
“단 한 번의 동작만으로 SOS 신호”를 보낼 수 있는 기술.

🔵 4. 현실 적용 가능성

MSA, Azure 클라우드, WebSocket 실시간 처리로
“실제 운영 가능한 완성된 구조”.

🚀 주요 기능
1) 🖐 실시간 동작·표정 인식

MediaPipe FaceLandmarker + Hands

100ms WebSocket 스트리밍

랜드마크 + Blendshape 실시간 분석

2) 🧠 DTW 기반 개인 맞춤 제스처 매칭

사용자별 제스처 사전 생성

RMS 정규화 + DTW로 정교한 매칭

장애 정도·신체 특성에 맞춰 인식률 최적화

3) 🤖 AI 문장 생성 – LangGraph 오케스트레이션
AI 모델	역할
Llama (Local)	Intent 분류, 욕설/오류 필터, fallback 생성
GPT(4.1-mini)	응급·일상 문장 생성, 존댓말 리라이팅
Gemini	안전성·길이·톤 검사, 문장 품질 보증

→ 3중 안전망으로 품질·안전·일관성을 동시에 달성

4) 🔊 문장·음성 이중 출력

텍스트 카드 UI

SpeechSynthesis 음성 출력

보호자/의료진 알림 기능

5) ☁ Azure 기반 Cloud-Native 아키텍처

Spring Boot + FastAPI 마이크로서비스

Azure Container App

자동 배포/롤백

CI/CD with GitHub Actions

🧩 전체 아키텍처 구조
User Camera
   ↓ (WebSocket 100ms)
MediaPipe JS
   ↓
API Gateway (Spring Boot)
   ↓
Motion Service (DTW Matching)
   ↓
AI Server (FastAPI + MediaPipe)
   ↓
LangGraph Orchestration
   ├── Llama Intent
   ├── GPT Generator
   └── Gemini Safety Checker
   ↓
Final Sentence
   ↓
Frontend (UI + 음성 + 보호자 알림)

📊 핵심 기술 요소 정리
🔷 Multimodal Processing

MediaPipe Landmark

Blendshape Vector

Joint 21개 기반 손동작 분석

🔷 Sequence Analysis

RMS Scale Normalization

DTW(Dynamic Time Warping)

개인별 동작 템플릿 매칭

🔷 LLM Orchestration

LangGraph State Machine

3-LLM pipeline

Prompt Rule Enforcement

🔷 Cloud & MSA

Azure Container Apps

Spring Boot WebFlux

FastAPI AI Microservice

WebSocket 실시간 스트림

👥 팀 역할 분담
역할	담당 내용
AI/ML Developer	MediaPipe 모델, FastAPI 서버, DTW 엔진, LLM 프롬프팅
Backend Developer	Spring Boot MSA, API Gateway, WebSocket, Azure 배포
Frontend Engineer	실시간 카메라 UI, 문장 출력, 음성 기능
PM & 데이터 기획	사용자 요구 분석, UX 플로우, 장애인 커뮤니케이션 리서치
🌍 사회적 임팩트 & 확장 전략 (3단계 로드맵)
① 시범 도입 (Pilot)

장애인 복지관 3곳

요양원/의료기관 테스트

사용자 300명 확보

② 전국 확산 (National Scale)

보건복지부·서울시 복지재단 협력

공공기관 MOU

연 3,000+ 사용자 확보

③ 글로벌 확장 (Global AAC Platform)

ASL(미국 수어)·JSL(일본 수어) 확장

다국어 멀티모달 AI

WHO·NGO 협력 가능성

💙 단순한 기술 프로젝트가 아니라,
사회적 약자의 ‘소통할 권리’를 실현하는 공공 혁신 모델입니다.

🔮 향후 발전 계획

전체 수어 문장 인식 모델 개발

감정 기반 비언어 신호 자동 해석

고령층 맞춤 UI & 음성 기반 인터랙션

사용자 건강/행동 패턴 기반 응급 감지
