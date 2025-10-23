package kopo.motionservice.service.impl;

import kopo.motionservice.dto.MotionRecordRequestDTO;
import kopo.motionservice.repository.RecordedMotionRepository;
import kopo.motionservice.repository.document.RecordedMotionDocument;
import kopo.motionservice.service.IMotionService;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;

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

        // TODO: JWT 인증 구현 후, 토큰에서 실제 userId를 가져와야 합니다.
        String mockUserId = "user123";

        // DTO -> Document 수동 매핑 시작
        MotionRecordRequestDTO.MotionDataDTO motionDataDTO = requestDTO.getMotionData();
        RecordedMotionDocument.MotionDataDocument motionDataDocument = new RecordedMotionDocument.MotionDataDocument();

        if (motionDataDTO != null) {
            // face_blendshapes 수동 매핑
            List<RecordedMotionDocument.FaceBlendshapesFrameDocument> blendshapesFrames = Optional.ofNullable(motionDataDTO.getFaceBlendshapes()).orElse(new ArrayList<>()).stream()
                    .map(dtoFrame -> {
                        RecordedMotionDocument.FaceBlendshapesFrameDocument docFrame = new RecordedMotionDocument.FaceBlendshapesFrameDocument();
                        docFrame.setTimestampMs(dtoFrame.getTimestampMs());
                        docFrame.setValues(dtoFrame.getValues());
                        return docFrame;
                    }).collect(Collectors.toList());
            motionDataDocument.setFaceBlendshapes(blendshapesFrames);

            // hand_landmarks 수동 매핑
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

        // 최종 Document 객체 생성
        RecordedMotionDocument document = RecordedMotionDocument.builder()
                .userId(mockUserId)
                .phrase(requestDTO.getPhrase())
                .motionType(requestDTO.getMotionType())
                .motionData(motionDataDocument)
                .build();

        // DB에 저장
        recordedMotionRepository.save(document);

        log.info("[MotionService] Motion saved successfully! recordId: {}", document.getRecordId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public String sendMotionVideoToFastAPI(
            String phrase,
            String detectionArea,
            org.springframework.web.multipart.MultipartFile videoFile,
            String trimStart,
            String trimEnd
    ) {
        log.info("[MotionService] Sending Multipart → FastAPI: phrase={}, area={}", phrase, detectionArea);

        // We no longer perform any client-side trimming or FFmpeg processing here.
        // Simply stream the uploaded MultipartFile to the FastAPI endpoint and
        // parse the returned JSON payload, then persist it into MongoDB.

        ObjectMapper mapper = new ObjectMapper();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            var requestFactory = new org.springframework.http.client.HttpComponentsClientHttpRequestFactory(httpClient);
            requestFactory.setConnectTimeout(10_000);
            requestFactory.setReadTimeout(120_000);

            // Wrap the MultipartFile input stream so RestTemplate can stream it
            InputStreamResource fileResource = new InputStreamResource(videoFile.getInputStream()) {
                @Override
                public String getFilename() {
                    String name = videoFile.getOriginalFilename();
                    return (name == null || name.isBlank()) ? "motion.webm" : name;
                }

                @Override
                public long contentLength() {
                    try {
                        long size = videoFile.getSize();
                        return size <= 0 ? -1 : size;
                    } catch (Exception e) {
                        return -1;
                    }
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("phrase", phrase);
            body.add("detectionArea", detectionArea);
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
            log.info("[MotionService] FastAPI responded: status={} bodyLen={}", resp.getStatusCode(), (respBody == null ? 0 : respBody.length()));

            if (respBody == null || respBody.isBlank()) {
                return "";
            }

            // Parse the JSON response into a Map
            Map<String, Object> parsed = mapper.readValue(respBody, new TypeReference<Map<String, Object>>() {});
            Object motionDataObj = parsed.get("motion_data");

            RecordedMotionDocument.MotionDataDocument motionDataDocument = new RecordedMotionDocument.MotionDataDocument();

            if (motionDataObj instanceof Map) {
                Map<String, Object> motionDataMap = (Map<String, Object>) motionDataObj;

                // face_blendshapes
                if (motionDataMap.containsKey("face_blendshapes")) {
                    Object fbObj = motionDataMap.get("face_blendshapes");
                    if (fbObj instanceof List) {
                        List<?> fbList = (List<?>) fbObj;
                        List<RecordedMotionDocument.FaceBlendshapesFrameDocument> fbDocs = new ArrayList<>();
                        for (Object item : fbList) {
                            if (!(item instanceof Map)) continue;
                            Map<String, Object> itemMap = (Map<String, Object>) item;
                            RecordedMotionDocument.FaceBlendshapesFrameDocument fdoc = new RecordedMotionDocument.FaceBlendshapesFrameDocument();
                            Number ts = (Number) itemMap.getOrDefault("timestamp_ms", 0);
                            fdoc.setTimestampMs(ts.longValue());

                            Object valuesObj = itemMap.get("values");
                            if (valuesObj instanceof Map) {
                                Map<String, Object> valuesMap = (Map<String, Object>) valuesObj;
                                // convert numbers to Double
                                Map<String, Double> converted = valuesMap.entrySet().stream()
                                        .filter(e -> e.getValue() instanceof Number)
                                        .collect(Collectors.toMap(Map.Entry::getKey, e -> ((Number) e.getValue()).doubleValue()));
                                fdoc.setValues(converted);
                            }

                            fbDocs.add(fdoc);
                        }
                        motionDataDocument.setFaceBlendshapes(fbDocs);
                    }
                }

                // hand_landmarks
                if (motionDataMap.containsKey("hand_landmarks")) {
                    Object hlObj = motionDataMap.get("hand_landmarks");
                    if (hlObj instanceof List) {
                        List<?> hlList = (List<?>) hlObj;
                        List<RecordedMotionDocument.HandLandmarksFrameDocument> hlDocs = new ArrayList<>();
                        for (Object item : hlList) {
                            if (!(item instanceof Map)) continue;
                            Map<String, Object> itemMap = (Map<String, Object>) item;
                            RecordedMotionDocument.HandLandmarksFrameDocument hdoc = new RecordedMotionDocument.HandLandmarksFrameDocument();
                            Number ts = (Number) itemMap.getOrDefault("timestamp_ms", 0);
                            hdoc.setTimestampMs(ts.longValue());

                            // right_hand and left_hand may be null or arrays
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
            }

            // Build final RecordedMotionDocument and save
            String mockUserId = "user123"; // TODO: replace with real user extraction from JWT

            RecordedMotionDocument document = RecordedMotionDocument.builder()
                    .userId(mockUserId)
                    .phrase((String) parsed.getOrDefault("phrase", phrase))
                    .motionType((String) parsed.getOrDefault("detectionArea", detectionArea))
                    .motionData(motionDataDocument)
                    .build();

            recordedMotionRepository.save(document);
            log.info("[MotionService] Saved motion to DB recordId={}", document.getRecordId());

            return respBody;

        } catch (Exception e) {
            log.error("[MotionService] Error sending video to FastAPI or saving result", e);
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public String sendMotionVideoToFastAPI(String phrase, String detectionArea, org.springframework.web.multipart.MultipartFile videoFile) {
        // Delegate to the main method with null trimStart/trimEnd
        return sendMotionVideoToFastAPI(phrase, detectionArea, videoFile, null, null);
    }

    @Override
    public List<RecordedMotionDocument> getAllRecordedMotions() {
        return recordedMotionRepository.findAll();
    }
}
