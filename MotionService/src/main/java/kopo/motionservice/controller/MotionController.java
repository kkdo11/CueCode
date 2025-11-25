package kopo.motionservice.controller;

import kopo.motionservice.service.IMotionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/motions")
@RequiredArgsConstructor
public class MotionController {
    private final IMotionService motionService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadMotionVideo(
            @RequestParam("motionLabel") String motionLabel,
            @RequestParam("detectionArea") String detectionArea,
            @RequestParam(value = "motionDescription", required = false) String motionDescription,
            @RequestParam("videoFile") MultipartFile videoFile,
            Principal principal) {

        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        String userId = principal.getName();

        String result = motionService.sendMotionVideoToFastAPI(motionLabel, detectionArea, videoFile, userId, motionDescription);
        return ResponseEntity.ok(result);
    }

    @Data
    public static class SentenceResponseDto {
        private final String sentence;
    }

    @PostMapping("/users/{userId}/sentence")
    public ResponseEntity<SentenceResponseDto> generateSentence(@PathVariable String userId) {
        log.info("[SentenceController] Generating sentence for userId={}", userId);
        String sentence = motionService.generateSentence(userId);
        return ResponseEntity.ok(new SentenceResponseDto(sentence));
    }
}
