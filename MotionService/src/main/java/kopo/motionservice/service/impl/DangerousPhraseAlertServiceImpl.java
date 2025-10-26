package kopo.motionservice.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.motionservice.repository.DangerousPhraseAlertRepository;
import kopo.motionservice.repository.document.DangerousPhraseAlertDocument;
import kopo.motionservice.service.IDangerousPhraseAlertService;
import kopo.motionservice.util.RedisUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DangerousPhraseAlertServiceImpl implements IDangerousPhraseAlertService {

    private final DangerousPhraseAlertRepository dangerousPhraseAlertRepository;
    private final RedisUtil redisUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.user-service.url}")
    private String userServiceUrl;

    private static final String REDIS_CACHE_PREFIX_TWO_HOURS = "dangerous_phrase_cache_two_hours:";
    private static final long REDIS_CACHE_TIMEOUT_SECONDS = 300; // 5 minutes

    @Override
    public List<DangerousPhraseAlertDocument> getDangerousPhraseAlertsForUser(String userId) {
        String redisKey = REDIS_CACHE_PREFIX_TWO_HOURS + userId;
        String cachedData = redisUtil.get(redisKey);

        if (cachedData != null) {
            try {
                log.info("[DangerousPhraseAlertServiceImpl] Loading dangerous phrase alerts from Redis for userId: {}", userId);
                return objectMapper.readValue(cachedData, new TypeReference<List<DangerousPhraseAlertDocument>>() {});
            } catch (Exception e) {
                log.warn("[DangerousPhraseAlertServiceImpl] Failed to load dangerous phrase alerts from Redis for userId {}: {}", userId, e.getMessage());
            }
        }

        log.info("[DangerousPhraseAlertServiceImpl] Loading dangerous phrase alerts from DB for userId: {}", userId);
        List<DangerousPhraseAlertDocument> alerts = fetchAlertsFromDb(userId);
        saveAlertsToRedis(userId, alerts);
        return alerts;
    }

    @Override
    public Optional<DangerousPhraseAlertDocument> getLatestAlertForManager(String managerId) {
        log.info("[DangerousPhraseAlertServiceImpl] Getting latest alert for managerId: {}", managerId);
        List<String> patientIds = getPatientIdsForManager(managerId);
        if (patientIds.isEmpty()) {
            return Optional.empty();
        }
        return dangerousPhraseAlertRepository.findTopByUserIdInAndConfirmedIsFalseOrderByDetectedTimeDesc(patientIds);
    }

    @Override
    public void confirmAlert(String alertId) {
        log.info("[DangerousPhraseAlertServiceImpl] Confirming alertId: {}", alertId);
        dangerousPhraseAlertRepository.findById(alertId).ifPresent(alert -> {
            alert.setConfirmed(true);
            dangerousPhraseAlertRepository.save(alert);
            log.info("Alert {} confirmed.", alertId);
        });
    }

    private List<String> getPatientIdsForManager(String managerId) {
        try {
            String url = userServiceUrl + "/patient/list?managerId=" + managerId;
            ResponseEntity<List<PatientInfoDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<PatientInfoDTO>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().stream()
                        .map(PatientInfoDTO::getId)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to get patient list for manager {}: {}", managerId, e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<DangerousPhraseAlertDocument> fetchAlertsFromDb(String userId) {
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        return dangerousPhraseAlertRepository.findByUserIdAndDetectedTimeAfter(userId, twoHoursAgo);
    }

    private void saveAlertsToRedis(String userId, List<DangerousPhraseAlertDocument> alerts) {
        String redisKey = REDIS_CACHE_PREFIX_TWO_HOURS + userId;
        try {
            String jsonAlerts = objectMapper.writeValueAsString(alerts);
            redisUtil.set(redisKey, jsonAlerts, REDIS_CACHE_TIMEOUT_SECONDS);
            log.info("[DangerousPhraseAlertServiceImpl] Dangerous phrase alerts saved to Redis for userId: {}", userId);
        } catch (Exception e) {
            log.error("[DangerousPhraseAlertServiceImpl] Failed to save dangerous phrase alerts to Redis for userId {}: {}", userId, e.getMessage());
        }
    }

    @Data
    private static class PatientInfoDTO {
        private String id;
    }
}
