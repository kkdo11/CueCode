package kopo.motionservice.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.motionservice.dto.LastPhraseDTO;
import kopo.motionservice.repository.PhraseHistoryRepository;
import kopo.motionservice.repository.document.PhraseHistoryDocument;
import kopo.motionservice.service.IPhraseHistoryService;
import kopo.motionservice.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhraseHistoryServiceImpl implements IPhraseHistoryService {

    private final PhraseHistoryRepository phraseHistoryRepository;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String REDIS_CACHE_PREFIX_TWO_HOURS = "phrase_history_cache_two_hours:";
    private static final long REDIS_CACHE_TIMEOUT_SECONDS = 300; // 5 minutes

    @Override
    public List<PhraseHistoryDocument> getPhraseHistoryForUser(String userId) {
        String redisKey = REDIS_CACHE_PREFIX_TWO_HOURS + userId;
        String cachedData = redisUtil.get(redisKey);

        if (cachedData != null) {
            try {
                log.info("[PhraseHistoryServiceImpl] Loading phrase history from Redis for userId: {}", userId);
                return objectMapper.readValue(cachedData, new TypeReference<List<PhraseHistoryDocument>>() {});
            } catch (Exception e) {
                log.warn("[PhraseHistoryServiceImpl] Failed to load phrase history from Redis for userId {}: {}", userId, e.getMessage());
                // Fallback to DB if Redis cache fails
            }
        }

        log.info("[PhraseHistoryServiceImpl] Loading phrase history from DB for userId: {}", userId);
        List<PhraseHistoryDocument> history = fetchHistoryFromDb(userId);
        saveHistoryToRedis(userId, history);
        return history;
    }

    @Override
    public LastPhraseDTO getLastPhraseForUser(String userId) {
        log.info("[PhraseHistoryServiceImpl] Getting last phrase for userId: {}", userId);
        Optional<PhraseHistoryDocument> lastPhraseDoc = phraseHistoryRepository.findTopByUserIdOrderByDetectedTimeDesc(userId);

        return lastPhraseDoc
                .map(doc -> new LastPhraseDTO(doc.getPhrase()))
                .orElse(new LastPhraseDTO("기록 없음"));
    }

    private List<PhraseHistoryDocument> fetchHistoryFromDb(String userId) {
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        return phraseHistoryRepository.findByUserIdAndDetectedTimeAfter(userId, twoHoursAgo);
    }

    private void saveHistoryToRedis(String userId, List<PhraseHistoryDocument> history) {
        String redisKey = REDIS_CACHE_PREFIX_TWO_HOURS + userId;
        try {
            String jsonHistory = objectMapper.writeValueAsString(history);
            redisUtil.set(redisKey, jsonHistory, REDIS_CACHE_TIMEOUT_SECONDS);
            log.info("[PhraseHistoryServiceImpl] Phrase history saved to Redis for userId: {}", userId);
        } catch (Exception e) {
            log.error("[PhraseHistoryServiceImpl] Failed to save phrase history to Redis for userId {}: {}", userId, e.getMessage());
        }
    }
}
