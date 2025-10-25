package kopo.motionservice.controller;

import kopo.motionservice.repository.document.DangerousPhraseAlertDocument;
import kopo.motionservice.service.IDangerousPhraseAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/motions/alerts")
@RequiredArgsConstructor
public class DangerousPhraseAlertController {

    private final IDangerousPhraseAlertService dangerousPhraseAlertService;

    @GetMapping("")
    public ResponseEntity<List<DangerousPhraseAlertDocument>> getDangerousPhraseAlerts(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = principal.getName();
        log.info("[DangerousPhraseAlertController] Request to get dangerous phrase alerts for userId: {}", userId);
        List<DangerousPhraseAlertDocument> alerts = dangerousPhraseAlertService.getDangerousPhraseAlertsForUser(userId);
        return ResponseEntity.ok(alerts);
    }
}
