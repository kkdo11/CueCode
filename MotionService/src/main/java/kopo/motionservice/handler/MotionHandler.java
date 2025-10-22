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

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("[MotionHandler] New client connected: {}", session.getId());
        buffers.put(session.getId(), new ArrayList<>());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        log.info("[MotionHandler] Client disconnected: {}", session.getId());
        buffers.remove(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("[MotionHandler] Received data from client {}: {}", session.getId(), payload);

        try {
            JsonNode root = mapper.readTree(payload);
            String type = root.has("type") ? root.get("type").asText() : "frame";

            if ("frame".equalsIgnoreCase(type)) {
                // expected shape: { type: 'frame', detectionArea: 'face', features: [0.1,0.2,...] }
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
                String detectionArea = root.has("detectionArea") ? root.get("detectionArea").asText() : null;
                List<double[]> seq = buffers.getOrDefault(session.getId(), Collections.emptyList());
                if (seq.isEmpty()) {
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of("type", "match", "matched", false, "reason", "empty_sequence"))));
                    return;
                }
                MatchResultDTO res = matchingService.matchSequence(seq, detectionArea == null ? "face" : detectionArea);
                Map<String, Object> out = new HashMap<>();
                out.put("type", "match");
                out.put("matched", res.getRecordId() != null);
                out.put("recordId", res.getRecordId());
                out.put("phrase", res.getPhrase());
                out.put("motionType", res.getMotionType());
                out.put("score", res.getScore());
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
