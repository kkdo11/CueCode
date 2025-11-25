// java
package kopo.motionservice.service.impl;

import kopo.motionservice.dto.MotionRecordRequestDTO;
import kopo.motionservice.repository.RecordedMotionRepository;
import kopo.motionservice.repository.document.RecordedMotionDocument;
import kopo.motionservice.service.IMotionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// 변경: javax -> jakarta
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;


@Slf4j
@Service
@RequiredArgsConstructor
public class MotionServiceImpl implements IMotionService {

    private final RecordedMotionRepository recordedMotionRepository;

    @Value("${cuecodemotion.url}")
    private String cuecodeMotionBaseUrl;

    @Override
    public void saveRecordedMotion(MotionRecordRequestDTO requestDTO) {
        log.info("[MotionService] Manual Mapping - Saving recorded motion for phrase: {}", requestDTO.getPhrase());

        String userId = Optional.ofNullable(extractUserIdFromJwt()).orElse("user123");
        log.info("[MotionService] Using userId={}", userId);

        MotionRecordRequestDTO.MotionDataDTO motionDataDTO = requestDTO.getMotionData();
        RecordedMotionDocument.MotionDataDocument motionDataDocument = new RecordedMotionDocument.MotionDataDocument();

        if (motionDataDTO != null) {
            List<RecordedMotionDocument.FaceBlendshapesFrameDocument> blendshapesFrames = Optional.ofNullable(motionDataDTO.getFaceBlendshapes()).orElse(new ArrayList<>()).stream()
                    .map(dtoFrame -> {
                        RecordedMotionDocument.FaceBlendshapesFrameDocument docFrame = new RecordedMotionDocument.FaceBlendshapesFrameDocument();
                        docFrame.setTimestampMs(dtoFrame.getTimestampMs());
                        docFrame.setValues(dtoFrame.getValues());
                        return docFrame;
                    }).collect(Collectors.toList());
            motionDataDocument.setFaceBlendshapes(blendshapesFrames);

            List<RecordedMotionDocument.HandLandmarksFrameDocument> landmarksFrames = Optional.ofNullable(motionDataDTO.getHandLandmarks()).orElse(new ArrayList<>()).stream()
                    .map(dtoFrame -> {
                        RecordedMotionDocument.HandLandmarksFrameDocument docFrame = new RecordedMotionDocument.HandLandmarksFrameDocument();
                        docFrame.setTimestampMs(dtoFrame.getTimestampMs());
                        docFrame.setLeftHand(dtoFrame.getLeftHand());
                        docFrame.setRightHand(dtoFrame.getRightHand());
                        return docFrame;
                    }).collect(Collectors.toList());
            motionDataDocument.setHandLandmarks(landmarksFrames);
        }

        RecordedMotionDocument document = RecordedMotionDocument.builder()
                .userId(userId)
                .phrase(requestDTO.getPhrase())
                .motionType(requestDTO.getMotionType())
                .motionData(motionDataDocument)
                .description(requestDTO.getDescription())
                .build();

        recordedMotionRepository.save(document);

        log.info("[MotionService] Motion saved successfully! recordId: {} userId: {}", document.getRecordId(), document.getUserId());
    }

//    @Override
//    public String sendMotionVideoToFastAPI(String phrase, String detectionArea, MultipartFile videoFile, String userId, String description) {
//        return "";
//    }

    // ===== IMPLEMENTATION: 5-arg method delegates to 7-arg (no duplicates) =====
    @Override
    public String sendMotionVideoToFastAPI(String phrase,
                                           String detectionArea,
                                           MultipartFile videoFile,
                                           String userId,
                                           String description) {
        // delegate; no trims
        return sendMotionVideoToFastAPI(phrase, detectionArea, videoFile, userId, null, null, description);
    }


    // JWT에서 payload의 "sub" 추출 (서명 검증 없음 — 프로덕션에서는 서명 검증 필요)
    private String extractUserIdFromJwt() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                log.debug("[MotionService] No request attributes available (not in HTTP request)");
                return null;
            }
            HttpServletRequest req = attrs.getRequest();
            String auth = req.getHeader("X-User-Id"); // 수정: Authorization -> X-User-Id
            log.debug("[MotionService] X-User-Id header={}", auth);
            if (auth == null) return null;
            return auth; // 헤더 값을 그대로 반환
        } catch (Exception e) {
            log.warn("[MotionService] Failed to extract userId from X-User-Id header", e);
            return null;
        }
    }



    @Override
    public String sendMotionVideoToFastAPI(String phrase,
                                           String detectionArea,
                                           org.springframework.web.multipart.MultipartFile videoFile,
                                           String userId,
                                           String trimStart,
                                           String trimEnd,
                                           String description) {
        log.info("[MotionService] Sending Multipart → FastAPI: phrase={}, area={}, userId={}, hasDesc={}",
                phrase, detectionArea, userId, (description != null && !description.isBlank()));

        ObjectMapper mapper = new ObjectMapper();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            var requestFactory = new org.springframework.http.client.HttpComponentsClientHttpRequestFactory(httpClient);
            requestFactory.setConnectTimeout(10_000);
            requestFactory.setReadTimeout(120_000);

            InputStreamResource fileResource = new InputStreamResource(videoFile.getInputStream()) {
                @Override public String getFilename() {
                    String name = videoFile.getOriginalFilename();
                    return (name == null || name.isBlank()) ? "motion.webm" : name;
                }
                @Override public long contentLength() {
                    try { long size = videoFile.getSize(); return size <= 0 ? -1 : size; }
                    catch (Exception e) { return -1; }
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("phrase", phrase);
            body.add("detectionArea", detectionArea);
            if (description != null && !description.isBlank())  {
                // Send with a conservative key; FastAPI can read either one if implemented:
                body.add("description", description);
                body.add("motionDescription", description);
            }
            if (trimStart != null) body.add("trimStart", trimStart);
            if (trimEnd != null) body.add("trimEnd", trimEnd);
            body.add("videoFile", fileResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            var rest = new org.springframework.web.client.RestTemplate(requestFactory);
            var req  = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = rest.exchange(
                    cuecodeMotionBaseUrl + "/api/process-motion",
                    HttpMethod.POST,
                    req,
                    String.class
            );

            String respBody = resp.getBody();
            log.info("[MotionService] FastAPI responded: status={} bodyLen={}",
                    resp.getStatusCode(), (respBody == null ? 0 : respBody.length()));

            // --- build motionDataDocument from resp (existing logic) ---
            Map<String, Object> parsed = (respBody == null || respBody.isBlank())
                    ? Collections.emptyMap()
                    : mapper.readValue(respBody, new TypeReference<Map<String, Object>>() {});

            RecordedMotionDocument.MotionDataDocument motionDataDocument = buildMotionDataFromParsed(parsed);

            // Persist (note: phrase/detectionArea may be overridden by FastAPI response if present)
            RecordedMotionDocument document = RecordedMotionDocument.builder()
                    .userId(userId)
                    .phrase((String) parsed.getOrDefault("phrase", phrase))
                    .motionType((String) parsed.getOrDefault("detectionArea", detectionArea))
                    .motionData(motionDataDocument)
                    .description(description)
                    .build();

            recordedMotionRepository.save(document);
            log.info("[MotionService] Saved motion to DB recordId={} userId={}", document.getRecordId(), document.getUserId());

            return (respBody == null) ? "" : respBody;

        } catch (Exception e) {
            log.error("[MotionService] Error sending video to FastAPI or saving result", e);
            return "Error: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private RecordedMotionDocument.MotionDataDocument buildMotionDataFromParsed(Map<String, Object> parsed) throws Exception {
        RecordedMotionDocument.MotionDataDocument motionDataDocument = new RecordedMotionDocument.MotionDataDocument();

        if (parsed == null || parsed.isEmpty()) {
            return motionDataDocument;
        }

        Object motionDataObj = parsed.get("motion_data");
        if (!(motionDataObj instanceof Map)) {
            return motionDataDocument;
        }

        Map<String, Object> motionDataMap = (Map<String, Object>) motionDataObj;

        // -------- face_blendshapes --------
        if (motionDataMap.containsKey("face_blendshapes")) {
            Object fbObj = motionDataMap.get("face_blendshapes");
            if (fbObj instanceof List) {
                List<?> fbList = (List<?>) fbObj;
                List<RecordedMotionDocument.FaceBlendshapesFrameDocument> fbDocs = new ArrayList<>();
                for (Object item : fbList) {
                    if (!(item instanceof Map)) continue;
                    Map<String, Object> itemMap = (Map<String, Object>) item;

                    RecordedMotionDocument.FaceBlendshapesFrameDocument fdoc =
                            new RecordedMotionDocument.FaceBlendshapesFrameDocument();

                    Number ts = (Number) itemMap.getOrDefault("timestamp_ms", 0);
                    fdoc.setTimestampMs(ts.longValue());

                    Object valuesObj = itemMap.get("values");
                    if (valuesObj instanceof Map) {
                        Map<String, Object> valuesMap = (Map<String, Object>) valuesObj;
                        Map<String, Double> converted = valuesMap.entrySet().stream()
                                .filter(e -> e.getValue() instanceof Number)
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> ((Number) e.getValue()).doubleValue()
                                ));
                        fdoc.setValues(converted);
                    }
                    fbDocs.add(fdoc);
                }
                motionDataDocument.setFaceBlendshapes(fbDocs);
            }
        }

        // -------- hand_landmarks --------
        if (motionDataMap.containsKey("hand_landmarks")) {
            Object hlObj = motionDataMap.get("hand_landmarks");
            if (hlObj instanceof List) {
                List<?> hlList = (List<?>) hlObj;
                List<RecordedMotionDocument.HandLandmarksFrameDocument> hlDocs = new ArrayList<>();
                for (Object item : hlList) {
                    if (!(item instanceof Map)) continue;
                    Map<String, Object> itemMap = (Map<String, Object>) item;

                    RecordedMotionDocument.HandLandmarksFrameDocument hdoc =
                            new RecordedMotionDocument.HandLandmarksFrameDocument();

                    Number ts = (Number) itemMap.getOrDefault("timestamp_ms", 0);
                    hdoc.setTimestampMs(ts.longValue());

                    // right_hand
                    Object rightObj = itemMap.get("right_hand");
                    if (rightObj instanceof List) {
                        List<?> outer = (List<?>) rightObj;
                        List<List<Double>> right = new ArrayList<>();
                        for (Object row : outer) {
                            if (row instanceof List) {
                                List<?> rowList = (List<?>) row;
                                List<Double> coords = new ArrayList<>();
                                for (Object v : rowList) {
                                    if (v instanceof Number) coords.add(((Number) v).doubleValue());
                                }
                                right.add(coords);
                            }
                        }
                        hdoc.setRightHand(right);
                    }

                    // left_hand
                    Object leftObj = itemMap.get("left_hand");
                    if (leftObj instanceof List) {
                        List<?> outer = (List<?>) leftObj;
                        List<List<Double>> left = new ArrayList<>();
                        for (Object row : outer) {
                            if (row instanceof List) {
                                List<?> rowList = (List<?>) row;
                                List<Double> coords = new ArrayList<>();
                                for (Object v : rowList) {
                                    if (v instanceof Number) coords.add(((Number) v).doubleValue());
                                }
                                left.add(coords);
                            }
                        }
                        hdoc.setLeftHand(left);
                    }

                    hlDocs.add(hdoc);
                }
                motionDataDocument.setHandLandmarks(hlDocs);
            }
        }

        return motionDataDocument;
    }



    @Override
    public List<RecordedMotionDocument> getAllRecordedMotions() {
        log.info("[MotionService] Fetching all recorded motions for caching.");
        return recordedMotionRepository.findAll();
    }

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:8000") // 또는 필요 시 host.docker.internal
            .build();

    @Data
    public static class SentenceRequest {
        private final String user_id;
    }

    @Data
    public static class SentenceResponse {
        private String sentence;
    }

    @Override
    public String generateSentence(String userId) {
        SentenceRequest req = new SentenceRequest(userId);
        SentenceResponse resp = webClient.post()
                .uri("/api/sentence/generate")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(SentenceResponse.class)
                .block();

        return (resp != null) ? resp.getSentence() : "";
    }

}
