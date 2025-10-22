package kopo.motionservice.service;

import kopo.motionservice.dto.MatchResultDTO;
import java.util.List;

public interface IMotionDetectorService {
    void reloadCache();
    MatchResultDTO matchSequence(List<double[]> liveSequence, String detectionArea);
    // Add other methods as needed from the implementation
}
