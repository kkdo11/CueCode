package kopo.motionservice.service.impl;

import kopo.motionservice.dto.MatchResultDTO;
import kopo.motionservice.repository.DangerousPhraseAlertRepository;
import kopo.motionservice.repository.document.DangerousPhraseAlertDocument;
import kopo.motionservice.repository.document.RecordedMotionDocument;
import kopo.motionservice.service.IMotionDetectorService;
import kopo.motionservice.service.IMotionService;
import kopo.motionservice.util.RedisUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import kopo.motionservice.repository.PhraseHistoryRepository;
import kopo.motionservice.repository.document.PhraseHistoryDocument;

@Slf4j
@Service
@RequiredArgsConstructor
public class MotionDetectorServiceImpl implements IMotionDetectorService {

    private final IMotionService motionService;
    private final PhraseHistoryRepository phraseHistoryRepository; // Add repository for history
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private DangerousPhraseAlertRepository dangerousPhraseAlertRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 사용자별 캐시: userId -> Map<recordId, CachedMotion>
    private final Map<String, Map<String, CachedMotion>> userCaches = new ConcurrentHashMap<>();

    private static final String REDIS_CACHE_PREFIX = "motion_cache:";
    private static final long REDIS_CACHE_TIMEOUT_SECONDS = 3600; // 1 hour

    private static final Set<String> DANGEROUS_PHRASES = new HashSet<>(Arrays.asList("도와주세요", "아파요"));

    @PostConstruct
    public void init() {
        log.info("[MotionDetectorServiceImpl] Initialized with user-based cache system.");
        // 초기화 시에는 빈 상태로 시작 - 각 사용자 연결 시 해당 사용자의 캐시를 로드
    }

    /**
     * 특정 사용자의 캐시를 리로드합니다.
     * userId가 null이면 전체 사용자의 데이터를 로드합니다.
     */
    public void reloadCache(String userId) {
        if (userId == null || userId.isEmpty()) {
            log.info("[MotionDetectorServiceImpl] Reloading cache for ALL users...");
            reloadAllUserCaches();
            return;
        }

        String redisKey = REDIS_CACHE_PREFIX + userId;
        String cachedData = redisUtil.get(redisKey);

        if (cachedData != null) {
            try {
                Map<String, CachedMotion> userCache = objectMapper.readValue(cachedData, new TypeReference<Map<String, CachedMotion>>() {});
                userCaches.put(userId, userCache);
                log.info("[MotionDetectorServiceImpl] Cache loaded from Redis for userId={}. {} motions cached.", userId, userCache.size());
                return;
            } catch (Exception e) {
                log.warn("[MotionDetectorServiceImpl] Failed to load cache from Redis for userId={}: {}", userId, e.getMessage());
                // Fallback to DB if Redis cache fails
            }
        }

        log.info("[MotionDetectorServiceImpl] Reloading cache from DB for userId={}...", userId);
        List<RecordedMotionDocument> all = motionService.getAllRecordedMotions();
        Map<String, CachedMotion> userCache = new HashMap<>();
        int loadedCount = 0;

        for (RecordedMotionDocument doc : all) {
            // userId가 일치하는 레코드만 처리
            if (!userId.equals(doc.getUserId())) {
                continue;
            }

            try {
                CachedMotion cm = buildCachedMotion(doc);
                if (cm != null) {
                    userCache.put(doc.getRecordId(), cm);
                    loadedCount++;
                }
            } catch (Exception e) {
                log.warn("[MotionDetectorServiceImpl] Failed to cache record {}: {}", doc.getRecordId(), e.getMessage());
            }
        }

        // 해당 사용자의 캐시를 업데이트
        userCaches.put(userId, userCache);
        log.info("[MotionDetectorServiceImpl] Cache loaded for userId={}. {} motions cached.", userId, loadedCount);

        // Save to Redis
        try {
            String jsonCache = objectMapper.writeValueAsString(userCache);
            redisUtil.set(redisKey, jsonCache, REDIS_CACHE_TIMEOUT_SECONDS);
            log.info("[MotionDetectorServiceImpl] Cache saved to Redis for userId={}", userId);
        } catch (Exception e) {
            log.error("[MotionDetectorServiceImpl] Failed to save cache to Redis for userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * 모든 사용자의 캐시를 리로드 (관리자 용도)
     */
    private void reloadAllUserCaches() {
        // For simplicity, reloadAllUserCaches will not use Redis for aggregated cache.
        // It will just reload from DB and update local cache.
        // Individual user caches will be saved to Redis when reloadCache(userId) is called.
        log.info("[MotionDetectorServiceImpl] Reloading all user caches from DB (Redis not used for aggregated cache)...");
        List<RecordedMotionDocument> all = motionService.getAllRecordedMotions();
        Map<String, Map<String, CachedMotion>> tempCaches = new HashMap<>();

        for (RecordedMotionDocument doc : all) {
            String docUserId = doc.getUserId();
            if (docUserId == null || docUserId.isEmpty()) {
                log.warn("[MotionDetectorServiceImpl] Skipping record {} with null/empty userId", doc.getRecordId());
                continue;
            }

            try {
                CachedMotion cm = buildCachedMotion(doc);
                if (cm != null) {
                    tempCaches.computeIfAbsent(docUserId, k -> new HashMap<>())
                              .put(doc.getRecordId(), cm);
                }
            } catch (Exception e) {
                log.warn("[MotionDetectorServiceImpl] Failed to cache record {}: {}", doc.getRecordId(), e.getMessage());
            }
        }

        userCaches.clear();
        userCaches.putAll(tempCaches);
        log.info("[MotionDetectorServiceImpl] Cache loaded for all users. {} users, total {} motions cached.",
                userCaches.size(),
                userCaches.values().stream().mapToInt(Map::size).sum());
    }

    @Override
    public MatchResultDTO matchSequence(List<double[]> liveSequence, String detectionArea, String userId) {
        if (liveSequence == null || liveSequence.isEmpty()) {
            log.debug("[MotionDetectorServiceImpl] Empty live sequence for userId={}", userId);
            return MatchResultDTO.noMatch();
        }

        // userId가 없으면 매칭 불가
        if (userId == null || userId.isEmpty()) {
            log.warn("[MotionDetectorServiceImpl] No userId provided for matching");
            return MatchResultDTO.noMatch();
        }

        // 해당 사용자의 캐시 가져오기
        Map<String, CachedMotion> userCache = userCaches.get(userId);

        // 캐시가 없으면 로드 시도
        if (userCache == null || userCache.isEmpty()) {
            log.info("[MotionDetectorServiceImpl] No cache found for userId={}. Loading cache...", userId);
            reloadCache(userId);
            userCache = userCaches.get(userId);

            if (userCache == null || userCache.isEmpty()) {
                log.warn("[MotionDetectorServiceImpl] No recorded motions found for userId={}", userId);
                return MatchResultDTO.noMatch();
            }
        }

        double bestScore = Double.POSITIVE_INFINITY;
        CachedMotion best = null;

        log.debug("[MotionDetectorServiceImpl] Matching against {} cached motions for userId={}", userCache.size(), userId);

        // 해당 사용자의 캐시에서만 매칭 수행
        for (CachedMotion cm : userCache.values()) {
            if (!matchesArea(cm.motionType, detectionArea)) continue;

            // If cached motion is hand-type, align and normalize the live sequence to cached dimensionality
            List<double[]> alignedLive = liveSequence;
            if (cm.motionType != null && cm.motionType.toLowerCase().contains("hand")) {
                int dims = (cm.sequence != null && cm.sequence.length > 0) ? cm.sequence[0].length : -1;
                if (dims <= 0) continue;
                alignedLive = alignAndNormalizeLiveHandSequence(liveSequence, dims);
            }

            double score = dtwDistance(alignedLive, cm.sequence);
            if (score < bestScore) {
                bestScore = score;
                best = cm;
            }
        }

        if (best == null) {
            log.debug("[MotionDetectorServiceImpl] No match found for userId={}, detectionArea={}", userId, detectionArea);
            return MatchResultDTO.noMatch();
        }

        log.info("[MotionDetectorServiceImpl] Match found for userId={}. recordId={}, phrase={}, score={}",
                userId, best.recordId, best.phrase, bestScore);

        // Check for dangerous phrases and save alert
        if (DANGEROUS_PHRASES.contains(best.phrase)) {
            log.warn("[MotionDetectorServiceImpl] Dangerous phrase detected for userId={}: {}", userId, best.phrase);
            DangerousPhraseAlertDocument alert = new DangerousPhraseAlertDocument(userId, best.phrase, LocalDateTime.now());
            dangerousPhraseAlertRepository.save(alert);
            log.info("[MotionDetectorServiceImpl] Dangerous phrase alert saved to MongoDB for userId={}", userId);
        }

        // Save all detected phrases to history
        try {
            log.info("[MotionDetectorServiceImpl] Saving phrase to history for userId={}: {}", userId, best.phrase);
            PhraseHistoryDocument historyDoc = new PhraseHistoryDocument();
            historyDoc.setUserId(userId);
            historyDoc.setPhrase(best.phrase);
            historyDoc.setDetectedTime(LocalDateTime.now());
            phraseHistoryRepository.save(historyDoc);
            log.info("[MotionDetectorServiceImpl] Phrase history saved to MongoDB for userId={}", userId);
        } catch (Exception e) {
            log.error("[MotionDetectorServiceImpl] Failed to save phrase history for userId={}: {}", userId, e.getMessage());
        }

        return new MatchResultDTO(best.recordId, best.phrase, best.motionType, bestScore);
    }

    @Override
    public MatchResultDTO matchSequence(List<double[]> liveSequence, String detectionArea) {
        // userId 없이 호출된 경우 - 레거시 호환성을 위해 유지하지만 경고 로그
        log.warn("[MotionDetectorServiceImpl] matchSequence called without userId - matching will fail");
        return MatchResultDTO.noMatch();
    }

    // simple matching rule: motionType contains detectionArea (case-insensitive)
    private boolean matchesArea(String motionType, String detectionArea) {
        if (motionType == null || detectionArea == null) return false;
        return motionType.toLowerCase().contains(detectionArea.toLowerCase());
    }

    private CachedMotion buildCachedMotion(RecordedMotionDocument doc) {
        if (doc.getMotionData() == null) return null;
        String motionType = doc.getMotionType();
        // Prefer face_blendshapes if available, otherwise use hand_landmarks
        if (doc.getMotionData().getFaceBlendshapes() != null && !doc.getMotionData().getFaceBlendshapes().isEmpty()) {
            // Build ordered list of keys across frames
            Set<String> keySet = new LinkedHashSet<>();
            doc.getMotionData().getFaceBlendshapes().forEach(f -> {
                if (f.getValues() != null) keySet.addAll(f.getValues().keySet());
            });
            List<String> keys = keySet.stream().sorted().toList();
            if (keys.isEmpty()) return null;

            List<double[]> seq = new ArrayList<>();
            for (RecordedMotionDocument.FaceBlendshapesFrameDocument f : doc.getMotionData().getFaceBlendshapes()) {
                double[] vec = new double[keys.size()];
                for (int i = 0; i < keys.size(); i++) {
                    Double v = f.getValues() == null ? null : f.getValues().get(keys.get(i));
                    vec[i] = (v == null) ? 0.0 : v;
                }
                seq.add(vec);
            }
            return new CachedMotion(doc.getRecordId(), doc.getPhrase(), motionType, seq.toArray(new double[0][]));
        }

        // hand landmarks: flatten [right_hand then left_hand] per frame
        if (doc.getMotionData().getHandLandmarks() != null && !doc.getMotionData().getHandLandmarks().isEmpty()) {
            // determine per-hand point count from first non-null entry
            int pointCount = -1; // points per hand
            for (RecordedMotionDocument.HandLandmarksFrameDocument h : doc.getMotionData().getHandLandmarks()) {
                if (h.getRightHand() != null) { pointCount = h.getRightHand().size(); break; }
                if (h.getLeftHand() != null) { pointCount = h.getLeftHand().size(); break; }
            }
            if (pointCount <= 0) return null;
            int dims = pointCount * 3 * 2; // right + left, each x,y,z

            List<double[]> seq = new ArrayList<>();
            for (RecordedMotionDocument.HandLandmarksFrameDocument h : doc.getMotionData().getHandLandmarks()) {
                double[] vec = new double[dims];
                // fill right hand
                if (h.getRightHand() != null) {
                    int idx = 0;
                    for (List<Double> p : h.getRightHand()) {
                        if (p != null && p.size() >= 3) {
                            vec[idx++] = p.get(0);
                            vec[idx++] = p.get(1);
                            vec[idx++] = p.get(2);
                        } else {
                            vec[idx++] = 0; vec[idx++] = 0; vec[idx++] = 0;
                        }
                    }
                    // pad remaining if fewer points
                    while (idx < pointCount * 3) { vec[idx++] = 0.0; }
                } else {
                    // right hand missing -> zeros for that block
                    Arrays.fill(vec, 0, pointCount * 3, 0.0);
                }
                // fill left hand
                if (h.getLeftHand() != null) {
                    int idx = pointCount * 3;
                    for (List<Double> p : h.getLeftHand()) {
                        if (p != null && p.size() >= 3) {
                            vec[idx++] = p.get(0);
                            vec[idx++] = p.get(1);
                            vec[idx++] = p.get(2);
                        } else {
                            vec[idx++] = 0; vec[idx++] = 0; vec[idx++] = 0;
                        }
                    }
                    while (idx < dims) { vec[idx++] = 0.0; }
                } else {
                    Arrays.fill(vec, pointCount * 3, dims, 0.0);
                }
                seq.add(vec);
            }

            // Normalize cached hand sequence per-frame (center & scale) to improve invariance
            List<double[]> normalized = normalizeHandSequence(seq, pointCount);
            return new CachedMotion(doc.getRecordId(), doc.getPhrase(), motionType, normalized.toArray(new double[0][]));
        }

        return null;
    }

    /**
     * Dynamic Time Warping distance between two sequences of feature vectors.
     * Returns a normalized distance (total cost divided by path length).
     */
    public double dtwDistance(List<double[]> a, double[][] b) {
        if (a == null || a.isEmpty() || b == null || b.length == 0) return Double.POSITIVE_INFINITY;
        int n = a.size();
        int m = b.length;
        double[][] dp = new double[n + 1][m + 1];
        for (int i = 0; i <= n; i++) Arrays.fill(dp[i], Double.POSITIVE_INFINITY);
        dp[0][0] = 0.0;

        for (int i = 1; i <= n; i++) {
            double[] ai = a.get(i - 1);
            for (int j = 1; j <= m; j++) {
                double[] bj = b[j - 1];
                double cost = euclidean(ai, bj);
                double minPrev = Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                dp[i][j] = cost + minPrev;
            }
        }

        double totalCost = dp[n][m];
        // simple normalization by (n+m)
        double norm = (n + m);
        return totalCost / Math.max(1.0, norm);
    }

    private double euclidean(double[] x, double[] y) {
        if (x == null || y == null) return Double.POSITIVE_INFINITY;
        int len = Math.min(x.length, y.length);
        double sum = 0.0;
        for (int i = 0; i < len; i++) {
            double d = x[i] - y[i];
            sum += d * d;
        }
        // account for length difference as penalty
        if (x.length != y.length) {
            int diff = Math.abs(x.length - y.length);
            sum += diff; // small penalty
        }
        return Math.sqrt(sum);
    }

    // --- new helpers for hand normalization & alignment ---

    // Normalize a cached hand sequence per-frame: center each hand block and scale to unit magnitude
    private List<double[]> normalizeHandSequence(List<double[]> seq, int pointCount) {
        if (seq == null) return Collections.emptyList();
        List<double[]> out = new ArrayList<>(seq.size());
        int dims = pointCount * 3 * 2;
        for (double[] frame : seq) {
            double[] f = padOrTrim(frame, dims);
            normalizeHandFrameInplace(f, pointCount);
            out.add(f);
        }
        return out;
    }

    // Align incoming live sequence frames to expected dims and normalize similarly
    private List<double[]> alignAndNormalizeLiveHandSequence(List<double[]> live, int expectedDims) {
        if (live == null) return Collections.emptyList();
        // expectedDims should be divisible by 6 (pointCount*3*2)
        int pointCount = (expectedDims / 3) / 2;
        List<double[]> out = new ArrayList<>(live.size());
        for (double[] raw : live) {
            double[] f = padOrTrim(raw, expectedDims);
            normalizeHandFrameInplace(f, pointCount);
            out.add(f);
        }
        return out;
    }

    // Ensure frame is exactly 'dims' long by padding zeros or truncating
    private double[] padOrTrim(double[] frame, int dims) {
        if (frame == null) return new double[dims];
        if (frame.length == dims) return Arrays.copyOf(frame, frame.length);
        double[] out = new double[dims];
        int copy = Math.min(dims, frame.length);
        System.arraycopy(frame, 0, out, 0, copy);
        if (copy < dims) Arrays.fill(out, copy, dims, 0.0);
        return out;
    }

    // Center each hand block (right and left) and scale by RMS of non-zero coords to reduce scale variance
    private void normalizeHandFrameInplace(double[] f, int pointCount) {
        if (f == null) return;
        int handBlock = pointCount * 3; // coords per hand
        // normalize right hand block
        normalizeHandBlock(f, 0, pointCount);
        // normalize left hand block
        normalizeHandBlock(f, handBlock, pointCount);
    }

    private void normalizeHandBlock(double[] f, int startIdx, int pointCount) {
        // compute centroid of present points
        double cx = 0, cy = 0, cz = 0; int count = 0;
        for (int p = 0; p < pointCount; p++) {
            int base = startIdx + p * 3;
            if (base + 2 >= f.length) break;
            double x = f[base], y = f[base + 1], z = f[base + 2];
            if (!isZeroPoint(x, y, z)) {
                cx += x; cy += y; cz += z; count++;
            }
        }
        if (count == 0) return; // nothing to normalize
        cx /= count; cy /= count; cz /= count;
        // subtract centroid
        double energy = 0.0;
        for (int p = 0; p < pointCount; p++) {
            int base = startIdx + p * 3;
            if (base + 2 >= f.length) break;
            double x = f[base], y = f[base + 1], z = f[base + 2];
            if (!isZeroPoint(x, y, z)) {
                double nx = x - cx; double ny = y - cy; double nz = z - cz;
                f[base] = nx; f[base + 1] = ny; f[base + 2] = nz;
                energy += nx*nx + ny*ny + nz*nz;
            } else {
                f[base] = 0; f[base + 1] = 0; f[base + 2] = 0;
            }
        }
        double scale = Math.sqrt(energy / Math.max(1, count));
        if (scale < 1e-6) scale = 1.0;
        for (int p = 0; p < pointCount; p++) {
            int base = startIdx + p * 3;
            if (base + 2 >= f.length) break;
            f[base] /= scale; f[base + 1] /= scale; f[base + 2] /= scale;
        }
    }

    private boolean isZeroPoint(double x, double y, double z) {
        return Math.abs(x) < 1e-8 && Math.abs(y) < 1e-8 && Math.abs(z) < 1e-8;
    }

    @Data
    @AllArgsConstructor
    public static class CachedMotion {
        private String recordId;
        private String phrase;
        private String motionType;
        private double[][] sequence; // precomputed per-frame feature vectors
    }
}
