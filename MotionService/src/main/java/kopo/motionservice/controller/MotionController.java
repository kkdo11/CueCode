package kopo.motionservice.controller;

import kopo.motionservice.service.IMotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/motions")
@RequiredArgsConstructor
public class MotionController {
    private final IMotionService motionService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadMotionVideo(
            @RequestParam("motionLabel") String motionLabel,
            @RequestParam("detectionArea") String detectionArea,
            @RequestParam("videoFile") MultipartFile videoFile) {
        String result = motionService.sendMotionVideoToFastAPI(motionLabel, detectionArea, videoFile);
        return ResponseEntity.ok(result);
    }
}
