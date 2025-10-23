package kopo.motionservice.service;

import kopo.motionservice.dto.MatchResultDTO;
import java.util.List;

public interface IMotionDetectorService {
    void reloadCache();
    // userId 매개변수를 추가합니다.
    MatchResultDTO matchSequence(List<double[]> liveSequence, String detectionArea, String userId);
    // Add other methods as needed from the implementation
}