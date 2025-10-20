package kopo.motionservice.controller;

import kopo.motionservice.dto.MotionRecordRequestDTO;
import kopo.motionservice.service.IMotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/motions")
@RequiredArgsConstructor
public class MotionRegisterController {

    private final IMotionService motionService;

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> registerMotion(@RequestBody MotionRecordRequestDTO requestDTO) {
        motionService.saveRecordedMotion(requestDTO);
        return ResponseEntity.ok("OK");
    }
}

