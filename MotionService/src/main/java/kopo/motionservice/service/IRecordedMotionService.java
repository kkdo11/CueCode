package kopo.motionservice.service;

import kopo.motionservice.dto.RecordedMotionDTO;
import java.util.List;

public interface IRecordedMotionService {
    List<RecordedMotionDTO> getRecordedMotionsByUserId(String userId);
    boolean deleteRecordedMotionsByUserId(String userId);
    // 삭제된 문서의 개수를 반환 (0이면 삭제 없음)
    long deleteRecordedMotionById(String motionId, String userId);
}
