// java
package kopo.motionservice.service;

import kopo.motionservice.dto.MatchResultDTO;
import java.util.List;

/**
 * Motion detector service contract.
 *
 * reloadCache(String userId)에서 userId가 null 또는 비어있으면 전체 재로딩을 의미합니다.
 * matchSequence(List<double[]>, String) 은 "현재 사용자"의 캐시를 사용해 매칭을 수행한다고 가정합니다.
 * 특정 사용자로 매칭해야 하는 경우 오버로드(matchSequence(..., String userId))를 사용합니다.
 */
public interface IMotionDetectorService {

    /**
     * Reload cache. If userId is null or empty, perform full reload.
     *
     * @param userId target user id, or null for full reload
     */
    void reloadCache(String userId);

    /**
     * Convenience full reload.
     */
    default void reloadCache() {
        reloadCache(null);
    }

    /**
     * Match live sequence against stored patterns for the current user (resolve userId from context).
     *
     * @param liveSequence live motion sequence
     * @param detectionArea detection area id/name
     * @return match result dto
     */
    MatchResultDTO matchSequence(List<double[]> liveSequence, String detectionArea);

    /**
     * Match live sequence against stored patterns for a specific user (explicit).
     *
     * @param liveSequence live motion sequence
     * @param detectionArea detection area id/name
     * @param userId target user id
     * @return match result dto
     */
    MatchResultDTO matchSequence(List<double[]> liveSequence, String detectionArea, String userId);
}
