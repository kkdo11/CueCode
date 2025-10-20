package kopo.motionservice.controller;

import kopo.motionservice.service.matching.MotionMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/motions")
@RequiredArgsConstructor
public class MotionCacheController {

    private final MotionMatchingService motionMatchingService;

    @PostMapping("/reload-cache")
    public ResponseEntity<String> reloadCache() {
        motionMatchingService.reloadCache();
        return ResponseEntity.ok("cache reloaded");
    }
}

