package kopo.motionservice.controller;

import kopo.motionservice.dto.LastPhraseDTO;
import kopo.motionservice.repository.document.PhraseHistoryDocument;
import kopo.motionservice.service.IPhraseHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/motions/history")
@RequiredArgsConstructor
public class PhraseHistoryController {

    private final IPhraseHistoryService phraseHistoryService;

    @GetMapping("")
    public ResponseEntity<List<PhraseHistoryDocument>> getPhraseHistory(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = principal.getName();
        log.info("[PhraseHistoryController] Request to get phrase history for userId: {}", userId);
        List<PhraseHistoryDocument> history = phraseHistoryService.getPhraseHistoryForUser(userId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/last")
    public ResponseEntity<LastPhraseDTO> getLastPhrase(@RequestParam("patientId") String patientId) {
        log.info("[PhraseHistoryController] Request to get last phrase for patientId: {}", patientId);
        LastPhraseDTO lastPhrase = phraseHistoryService.getLastPhraseForUser(patientId);
        return ResponseEntity.ok(lastPhrase);
    }
}
