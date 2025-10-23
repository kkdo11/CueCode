// java
package kopo.motionservice.controller;

import kopo.motionservice.dto.MotionRecordRequestDTO;
import kopo.motionservice.service.IMotionService;
import kopo.motionservice.service.IMotionDetectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/motions")
@RequiredArgsConstructor
public class MotionDetectorController {

    private final IMotionService motionService;
    private final IMotionDetectorService motionDetectorService;

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> registerMotion(@RequestBody MotionRecordRequestDTO requestDTO) {
        motionService.saveRecordedMotion(requestDTO);
        // requestDTO.getUserId()를 전달해 해당 사용자 캐시만 재로딩
        motionDetectorService.reloadCache(requestDTO.getUserId());
        return ResponseEntity.ok("OK");
    }

    // optional userId 파라미터 추가: 있으면 해당 사용자만, 없으면 전체 리로드
    @PostMapping("/reload-cache")
    public ResponseEntity<String> reloadCache(@RequestParam(required = false) String userId) {
        motionDetectorService.reloadCache(userId);
        return ResponseEntity.ok("cache reloaded");
    }
}
