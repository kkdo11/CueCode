package kopo.motionservice.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.motionservice.repository.DangerousPhraseAlertRepository;
import kopo.motionservice.repository.document.DangerousPhraseAlertDocument;
import kopo.motionservice.service.IDangerousPhraseAlertService;
import kopo.motionservice.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DangerousPhraseAlertServiceImpl implements IDangerousPhraseAlertService {

    private final DangerousPhraseAlertRepository dangerousPhraseAlertRepository;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                // Fallback to DB if Redis cache fails
            }
        }

        log.info("[DangerousPhraseAlertServiceImpl] Loading dangerous phrase alerts from DB for userId: {}", userId);
        List<DangerousPhraseAlertDocument> alerts = fetchAlertsFromDb(userId);
        saveAlertsToRedis(userId, alerts);
        return alerts;
    }

    @Override
    @Scheduled(fixedRate = 300000) // 5 minutes = 300000 ms
    public void refreshDangerousPhraseAlertCache(String userId) {
        log.info("[DangerousPhraseAlertServiceImpl] Refreshing dangerous phrase alerts cache for userId: {}", userId);
        List<DangerousPhraseAlertDocument> alerts = fetchAlertsFromDb(userId);
        saveAlertsToRedis(userId, alerts);
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
}
