let API_BASE; // Original variable name

if (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1") {
    // 로컬 개발 환경: API Gateway의 로컬 주소 + /api 접두사
    API_BASE = "http://localhost:13000/api";
} else {
    // 배포 환경: FrontUI와 동일한 호스트를 사용하고, Ingress가 /api 경로를 API Gateway로 라우팅한다고 가정
    // 만약 API Gateway가 별도의 서브도메인(예: api.cuecode.kr)을 사용한다면, 해당 주소로 변경해야 합니다.
    // 예: API_BASE = "https://api.cuecode.kr/api";
    API_BASE = window.location.protocol + "//" + window.location.host + "/api";
}

// 웹소켓 주소는 API_BASE를 기반으로 프로토콜만 변경
// API_BASE에 이미 /api가 포함되어 있으므로, WEBSOCKET_BASE_URL에도 /api가 포함됩니다.
const WEBSOCKET_BASE_URL = API_BASE.replace('http', 'ws').replace('https', 'wss');

// 전역 변수로 노출 (다른 JS 파일에서 사용 가능하도록)
window.API_BASE = API_BASE; // Original variable name
window.WEBSOCKET_BASE_URL = WEBSOCKET_BASE_URL;
