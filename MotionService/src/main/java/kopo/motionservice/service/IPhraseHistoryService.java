package kopo.motionservice.service;

import kopo.motionservice.dto.LastPhraseDTO;
import kopo.motionservice.repository.document.PhraseHistoryDocument;

import java.util.List;

public interface IPhraseHistoryService {
    List<PhraseHistoryDocument> getPhraseHistoryForUser(String userId);

    LastPhraseDTO getLastPhraseForUser(String userId);
}
