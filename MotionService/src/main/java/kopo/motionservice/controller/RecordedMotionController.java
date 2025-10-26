package kopo.motionservice.controller;

import kopo.motionservice.dto.RecordedMotionDTO;
import kopo.motionservice.service.IRecordedMotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/recorded-motions")
@RequiredArgsConstructor
public class RecordedMotionController {

    private final IRecordedMotionService recordedMotionService;

    /**
     * GET /api/v1/recorded-motions
     * 인증된 사용자 본인의 녹화된 모션 목록을 조회합니다.
     * @AuthenticationPrincipal을 사용하여 Security Context에서 사용자 ID를 안전하게 추출합니다.
     */
    @GetMapping // 경로 변수 제거
    public ResponseEntity<List<RecordedMotionDTO>> getRecordedMotions(@AuthenticationPrincipal String userId) {
        log.info("[RecordedMotionController] Attempting to get motions for authenticated userId: {}", userId);

        if (userId == null) {
            // 이 코드는 Security Filter에서 걸러져야 하지만, 방어적인 코드로 추가
            log.warn("[RecordedMotionController] Authentication Principal is missing. Returning 401.");
            return ResponseEntity.status(401).build();
        }

        try {
            // 추출된 userId를 사용하여 서비스 계층에서 데이터를 필터링합니다.
            List<RecordedMotionDTO> motions = recordedMotionService.getRecordedMotionsByUserId(userId);
            return ResponseEntity.ok(motions);
        } catch (Exception e) {
            log.error("[RecordedMotionController] Error getting recorded motions for authenticated userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * DELETE /api/v1/recorded-motions
     * 인증된 사용자 본인의 모든 녹화된 모션을 삭제합니다.
     * @AuthenticationPrincipal을 사용하여 Security Context에서 사용자 ID를 안전하게 추출합니다.
     */
    @DeleteMapping // 경로 변수 제거
    public ResponseEntity<String> deleteRecordedMotions(@AuthenticationPrincipal String userId) {
        log.info("[RecordedMotionController] Attempting to delete motions for authenticated userId: {}", userId);

        if (userId == null) {
            // 이 코드는 Security Filter에서 걸러져야 하지만, 방어적인 코드로 추가
            log.warn("[RecordedMotionController] Authentication Principal is missing. Returning 401.");
            return ResponseEntity.status(401).body("Authentication required.");
        }

        try {
            // 추출된 userId를 사용하여 서비스 계층에서 삭제 대상을 필터링합니다.
            boolean success = recordedMotionService.deleteRecordedMotionsByUserId(userId);
            if (success) {
                log.info("[RecordedMotionController] Successfully deleted motions for userId: {}", userId);
                return ResponseEntity.ok("Successfully deleted recorded motions for authenticated user: " + userId);
            } else {
                log.info("[RecordedMotionController] No motions found or deleted for userId: {}", userId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("[RecordedMotionController] Error deleting recorded motions for authenticated userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing deletion.");
        }
    }

    /**
     * DELETE /api/v1/recorded-motions/{motionId}
     * 인증된 사용자가 소유한 단일 녹화 모션을 ID로 삭제합니다.
     */
    @DeleteMapping("/{motionId}")
    public ResponseEntity<String> deleteRecordedMotionById(@PathVariable String motionId, @AuthenticationPrincipal String userId) {
        log.info("[RecordedMotionController] Attempting to delete single motion {} for authenticated userId: {}", motionId, userId);

        if (userId == null) {
            log.warn("[RecordedMotionController] Authentication Principal is missing. Returning 401.");
            return ResponseEntity.status(401).body("Authentication required.");
        }

        if (motionId == null || motionId.isBlank()) {
            log.warn("[RecordedMotionController] motionId is null or blank");
            return ResponseEntity.badRequest().body("motionId is required.");
        }

        try {
            long deletedCount = recordedMotionService.deleteRecordedMotionById(motionId, userId);
            if (deletedCount > 0) {
                log.info("[RecordedMotionController] Successfully deleted {} document(s) for motion {} and userId: {}", deletedCount, motionId, userId);
                return ResponseEntity.ok(String.format("Deleted %d document(s) for motion: %s", deletedCount, motionId));
            } else {
                log.info("[RecordedMotionController] Motion {} not found or not owned by user {}", motionId, userId);
                return ResponseEntity.status(404).body("Not Found");
            }
        } catch (Exception e) {
            log.error("[RecordedMotionController] Error deleting motion {} for userId {}: {}", motionId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing deletion.");
        }
    }
}
