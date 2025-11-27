package kopo.motionservice.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import kopo.motionservice.dto.MotionRecordRequestDTO;
import kopo.motionservice.repository.RecordedMotionRepository;
import kopo.motionservice.repository.document.RecordedMotionDocument;
import kopo.motionservice.service.IMotionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class MotionServiceImpl implements IMotionService {

    private final RecordedMotionRepository recordedMotionRepository;

    @Value("${cuecodemotion.url}")
    private String cuecodeMotionBaseUrl;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder().baseUrl(this.cuecodeMotionBaseUrl).build();
    }


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

    @Override
    public String sendMotionVideoToFastAPI(String phrase, String detectionArea, MultipartFile videoFile, String userId, String description) {
        return sendMotionVideoToFastAPI(phrase, detectionArea, videoFile, userId, null, null, description);
    }

    private String extractUserIdFromJwt() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                log.debug("[MotionService] No request attributes available (not in HTTP request)");
                return null;
            }
            HttpServletRequest req = attrs.getRequest();
            String auth = req.getHeader("X-User-Id");
            log.debug("[MotionService] X-User-Id header={}", auth);
            if (auth == null) return null;
            return auth;
        } catch (Exception e) {
            log.warn("[MotionService] Failed to extract userId from X-User-Id header", e);
            return null;
        }
    }

    @Override
    public String sendMotionVideoToFastAPI(String phrase,
                                           String detectionArea,
                                           MultipartFile videoFile,
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
                    return videoFile.getSize() > 0 ? videoFile.getSize() : -1;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("phrase", phrase);
            body.add("detectionArea", detectionArea);
            if (description != null && !description.isBlank())  {
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

            Map<String, Object> parsed = (respBody == null || respBody.isBlank())
                    ? Collections.emptyMap()
                    : mapper.readValue(respBody, new TypeReference<>() {});

            RecordedMotionDocument.MotionDataDocument motionDataDocument = buildMotionDataFromParsed(parsed);

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
    private RecordedMotionDocument.MotionDataDocument buildMotionDataFromParsed(Map<String, Object> parsed) {
        RecordedMotionDocument.MotionDataDocument motionDataDocument = new RecordedMotionDocument.MotionDataDocument();
        if (parsed == null || !parsed.containsKey("motion_data") || !(parsed.get("motion_data") instanceof Map)) {
            return motionDataDocument;
        }

        Map<String, Object> motionDataMap = (Map<String, Object>) parsed.get("motion_data");

        if (motionDataMap.containsKey("face_blendshapes") && motionDataMap.get("face_blendshapes") instanceof List) {
            List<Map<String, Object>> fbList = (List<Map<String, Object>>) motionDataMap.get("face_blendshapes");
            motionDataDocument.setFaceBlendshapes(fbList.stream().map(itemMap -> {
                RecordedMotionDocument.FaceBlendshapesFrameDocument fdoc = new RecordedMotionDocument.FaceBlendshapesFrameDocument();
                fdoc.setTimestampMs(((Number) itemMap.getOrDefault("timestamp_ms", 0)).longValue());
                if (itemMap.get("values") instanceof Map) {
                    Map<String, Number> valuesMap = (Map<String, Number>) itemMap.get("values");
                    fdoc.setValues(valuesMap.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().doubleValue())));
                }
                return fdoc;
            }).collect(Collectors.toList()));
        }

        if (motionDataMap.containsKey("hand_landmarks") && motionDataMap.get("hand_landmarks") instanceof List) {
            List<Map<String, Object>> hlList = (List<Map<String, Object>>) motionDataMap.get("hand_landmarks");
            motionDataDocument.setHandLandmarks(hlList.stream().map(itemMap -> {
                RecordedMotionDocument.HandLandmarksFrameDocument hdoc = new RecordedMotionDocument.HandLandmarksFrameDocument();
                hdoc.setTimestampMs(((Number) itemMap.getOrDefault("timestamp_ms", 0)).longValue());
                if (itemMap.get("right_hand") instanceof List) {
                    hdoc.setRightHand((List<List<Double>>) itemMap.get("right_hand"));
                }
                if (itemMap.get("left_hand") instanceof List) {
                    hdoc.setLeftHand((List<List<Double>>) itemMap.get("left_hand"));
                }
                return hdoc;
            }).collect(Collectors.toList()));
        }
        return motionDataDocument;
    }

    @Override
    public List<RecordedMotionDocument> getAllRecordedMotions() {
        log.info("[MotionService] Fetching all recorded motions for caching.");
        return recordedMotionRepository.findAll();
    }

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
