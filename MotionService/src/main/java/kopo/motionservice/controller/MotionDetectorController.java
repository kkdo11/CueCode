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
        // ensure newly saved motion is available for matching
        motionDetectorService.reloadCache();
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/reload-cache")
    public ResponseEntity<String> reloadCache() {
        motionDetectorService.reloadCache();
        return ResponseEntity.ok("cache reloaded");
    }
}

