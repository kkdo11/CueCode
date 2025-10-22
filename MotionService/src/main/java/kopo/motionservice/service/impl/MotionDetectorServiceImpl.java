package kopo.motionservice.service.impl;

import kopo.motionservice.dto.MatchResultDTO;
import kopo.motionservice.repository.document.RecordedMotionDocument;
import kopo.motionservice.service.IMotionDetectorService;
import kopo.motionservice.service.IMotionService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MotionDetectorServiceImpl implements IMotionDetectorService {

    private final IMotionService motionService;

    // In-memory cache of precomputed sequences by recordId
    private final Map<String, CachedMotion> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        reloadCache();
    }

    public void reloadCache() {
        log.info("[MotionDetectorServiceImpl] Reloading cache from DB via IMotionService...");
        List<RecordedMotionDocument> all = motionService.getAllRecordedMotions();
        Map<String, CachedMotion> tmp = new HashMap<>();
        for (RecordedMotionDocument doc : all) {
            try {
                CachedMotion cm = buildCachedMotion(doc);
                if (cm != null) tmp.put(doc.getRecordId(), cm);
            } catch (Exception e) {
                log.warn("[MotionDetectorServiceImpl] Failed to cache record {}: {}", doc.getRecordId(), e.getMessage());
            }
        }
        cache.clear();
        cache.putAll(tmp);
        log.info("[MotionDetectorServiceImpl] Cache loaded. {} motions cached.", cache.size());
    }

    @Override
    public MatchResultDTO matchSequence(List<double[]> liveSequence, String detectionArea) {
        if (liveSequence == null || liveSequence.isEmpty()) return MatchResultDTO.noMatch();

        double bestScore = Double.POSITIVE_INFINITY;
        CachedMotion best = null;

        for (CachedMotion cm : cache.values()) {
            if (!matchesArea(cm.motionType, detectionArea)) continue;
            double score = dtwDistance(liveSequence, cm.sequence);
            if (score < bestScore) {
                bestScore = score;
                best = cm;
            }
        }

        if (best == null) return MatchResultDTO.noMatch();

        return new MatchResultDTO(best.recordId, best.phrase, best.motionType, bestScore);
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
            return new CachedMotion(doc.getRecordId(), doc.getPhrase(), motionType, seq.toArray(new double[0][]));
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

    @Data
    @AllArgsConstructor
    public static class CachedMotion {
        private String recordId;
        private String phrase;
        private String motionType;
        private double[][] sequence; // precomputed per-frame feature vectors
    }
}
