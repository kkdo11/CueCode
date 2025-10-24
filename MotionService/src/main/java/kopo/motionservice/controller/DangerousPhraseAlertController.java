package kopo.motionservice.controller;

import kopo.motionservice.repository.document.DangerousPhraseAlertDocument;
import kopo.motionservice.service.IDangerousPhraseAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/motion/alerts")
@RequiredArgsConstructor
public class DangerousPhraseAlertController {

    private final IDangerousPhraseAlertService dangerousPhraseAlertService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<DangerousPhraseAlertDocument>> getDangerousPhraseAlerts(@PathVariable String userId) {
        log.info("[DangerousPhraseAlertController] Request to get dangerous phrase alerts for userId: {}", userId);
        List<DangerousPhraseAlertDocument> alerts = dangerousPhraseAlertService.getDangerousPhraseAlertsForUser(userId);
        return ResponseEntity.ok(alerts);
    }
}
