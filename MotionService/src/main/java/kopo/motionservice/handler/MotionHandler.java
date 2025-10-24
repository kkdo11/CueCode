package kopo.motionservice.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.motionservice.dto.MatchResultDTO;
import kopo.motionservice.service.IMotionDetectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.lang.NonNull; // Nullability 어노테이션 추가
import java.security.Principal; // Principal 객체 사용을 위한 Import 추가

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Component
public class MotionHandler extends TextWebSocketHandler {

    private final IMotionDetectorService matchingService;
    private final ObjectMapper mapper = new ObjectMapper();

    // per-session buffers of frames (each frame is double[] feature vector)
    private final Map<String, List<double[]>> buffers = new ConcurrentHashMap<>();

    // MatchResultDTO를 응답 Map으로 변환하는 헬퍼 함수
    private Map<String, Object> buildMatchResponse(MatchResultDTO res) {
        Map<String, Object> out = new HashMap<>();
        out.put("type", "match");
        out.put("matched", res.getRecordId() != null);
        out.put("recordId", res.getRecordId());
        out.put("phrase", res.getPhrase());
        out.put("motionType", res.getMotionType());
        out.put("score", res.getScore());
        return out;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        // userId 추출 (attributes에서 가져오기 - HttpHandshakeInterceptor가 설정함)
        String userId = (String) session.getAttributes().get("userId");
        if (userId == null) {
            userId = Optional.ofNullable(session.getPrincipal())
                    .map(Principal::getName)
                    .orElse("anonymous");
        }

        log.info("[MotionHandler] New client connected: {}, userId: {}", session.getId(), userId);

        // 해당 사용자의 캐시를 로드 (중요!)
        if (userId != null && !"anonymous".equals(userId)) {
            try {
                log.info("[MotionHandler] Loading cache for userId: {}", userId);
                matchingService.reloadCache(userId);
            } catch (Exception e) {
                log.error("[MotionHandler] Failed to load cache for userId: {}", userId, e);
            }
        }

        buffers.put(session.getId(), new ArrayList<>());
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull org.springframework.web.socket.CloseStatus status) {
        log.info("[MotionHandler] Client disconnected: {}", session.getId());
        buffers.remove(session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        String payload = message.getPayload();

        // userId 추출 (attributes에서 가져오기)
        String userId = (String) session.getAttributes().get("userId");
        if (userId == null) {
            userId = Optional.ofNullable(session.getPrincipal())
                    .map(Principal::getName)
                    .orElse(null);
        }

        // 로깅에 userId 포함
        log.info("[MotionHandler] Received data from client {}({}): {}", session.getId(), userId, payload);

        try {
            JsonNode root = mapper.readTree(payload);
            String type = root.has("type") ? root.get("type").asText() : "frame";

            if ("frame".equalsIgnoreCase(type)) {
                JsonNode featuresNode = root.get("features");
                if (featuresNode == null || !featuresNode.isArray()) {
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of("error", "missing features array"))));
                    return;
                }
                double[] feat = new double[featuresNode.size()];
                for (int i = 0; i < featuresNode.size(); i++) {
                    feat[i] = featuresNode.get(i).asDouble(0.0);
                }
                buffers.computeIfAbsent(session.getId(), k -> new ArrayList<>()).add(feat);

                // Optionally acknowledge
                session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of("type", "ack", "buffered", buffers.get(session.getId()).size()))));
                return;
            }

            if ("end".equalsIgnoreCase(type)) {
                // 'end'가 왔을 때만 매칭 수행
                String detectionArea = root.has("detectionArea") ? root.get("detectionArea").asText() : null;
                List<double[]> seq = buffers.getOrDefault(session.getId(), Collections.emptyList());

                if (seq.isEmpty()) {
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of("type", "match", "matched", false, "reason", "empty_sequence"))));
                    return;
                }

                // ✨ userId를 포함하여 매칭 서비스 호출 (중요!)
                MatchResultDTO res = matchingService.matchSequence(seq, detectionArea == null ? "face" : detectionArea, userId);

                // 응답 전송
                Map<String, Object> out = buildMatchResponse(res);
                session.sendMessage(new TextMessage(mapper.writeValueAsString(out)));

                // clear buffer after end
                buffers.remove(session.getId());
                return;
            }

            // unknown type: echo
            session.sendMessage(new TextMessage("ECHO: " + payload));

        } catch (Exception e) {
            log.error("[MotionHandler] Error processing message", e);
            session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of("error", e.getMessage()))));
        }
    }
}