# 🌐 CueCode – 수어·표정·제스처 기반 실시간 AI 멀티모달 커뮤니케이션 플랫폼

**“말하지 못해도, 표현할 수 있어야 한다.”**
주제: Redesign Everything with AI (AI와 함께 만드는 새로운 변화)

<p align="center">
<img src="https://img.shields.io/badge/AI-Multimodal-blue?style=flat-square" />
<img src="https://img.shields.io/badge/MSA-SpringBoot-green?style=flat-square" />
<img src="https://img.shields.io/badge/LLM-Orchestration-orange?style=flat-square" />
<img src="https://img.shields.io/badge/Backend-FastAPI-yellow?style=flat-square" />
</p>

---

## 1️⃣ 프로젝트 개요

### ❗ 해결하려는 문제

대한민국 내 약 **140만 명의 언어·청각·뇌병변 장애인**, 그리고 **고령화로 인해 급격히 증가하는 후천적 의사소통 취약계층**

**이들이 겪는 어려움:**

* 말을 하지 못함
* 손을 자유롭게 사용하기 어려움
* UI 조작이 어려움
* 긴급 상황에서 도움 요청이 늦어짐

**기존 AAC의 구조적 한계:**
> “터치 기반, 정적 문장, 복잡한 UI, 비언어 인식 불가”

---

## 2️⃣ CueCode가 제시하는 해결책

✔ **실시간 비언어 인식 → 언어 변환:** 수어·표정·손동작을 실시간으로 인식하여 자연스러운 문장/음성으로 변환.
✔ **개인 맞춤형 AI 커뮤니케이션 사전:** 사용자가 직접 만든 동작을 문장으로 학습 → 완전 개인화된 소통 방식 제공
✔ **위기 상황 자동 생성 문장:** AI가 위험 패턴을 감지하면 “도와주세요”, “숨이 안 쉬어져요” 등 즉시 생성·출력.
✔ **의료·복지·요양 현장에서 바로 활용:** 인터넷과 웹캠만 있으면 실행 가능한 경량 서비스 구조.

---

## 3️⃣ 핵심 가치

* 🔵 **디지털 접근성 확대:** 클릭 중심 AAC의 한계를 극복한 차세대 커뮤니케이션 패러다임.
* 🔵 **AI가 대신 말해주는 시스템:** 사용자의 비언어 신호를 해석해 자연스러운 문장으로 변환.
* 🔵 **긴급 상황 즉시 대응:** 즉시 문장 생성 + 음성 출력 → 생존에 직결되는 의사표현 가능.
* 🔵 **안정적인 운영:** Azure 기반 컨테이너 + FastAPI + SpringBoot + WebSocket을 통한 실서비스 구조.

---

## 4️⃣ 주요 기능

### 🖐 실시간 동작·표정 인식
* **MediaPipe** Face/Hand 모델 적용
* 52 Blendshape, 손 관절 21개 좌표 추출
* **WebSocket 100ms** 스트리밍 처리

### 🔢 DTW 기반 동작 매칭
* 동작 좌표 정규화
* **Dynamic Time Warping (DTW)** 시퀀스 비교
* 사용자 맞춤형 동작 템플릿

### 🤖 LangGraph 기반 LLM 오케스트레이션

| 모델 | 역할 |
| :--- | :--- |
| **Llama (Local)** | Intent 분류, 비속어/이상 패턴 필터링, GPT 장애 시 Fallback 생성 |
| **GPT-4.1-mini** | 문장 생성 (응급/일상), 존댓말 변환, 자연스러운 한국어 최적화 |
| **Gemini** | 의미·톤·문장 길이 검증, “안전성 체크 + 규칙 준수” 심판 역할 |

> **결과:** 3중 AI 검증의 안정적인 문장 생성 구조 구축

### 🔊 문장·음성 출력
* 화면 UI 카드 출력
* SpeechSynthesis 음성 출력

---

## 5️⃣ 시스템 아키텍처

```mermaid
graph TD
    A[사용자] --> B(MediaPipe JS);
    B --> C(WebSocket 100ms);
    C --> D[Spring Boot API Gateway];
    D --> E[Motion Service - DTW Matching];
    D --> F[FastAPI - MediaPipe AI 처리];
    E --> G[LangGraph Orchestration];
    F --> G;
    G --> H[결과 문장 출력 + 음성 피드백];
