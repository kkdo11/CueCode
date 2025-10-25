package kopo.motionservice.service;

import kopo.motionservice.repository.document.PhraseHistoryDocument;

import java.util.List;

public interface IPhraseHistoryService {
    List<PhraseHistoryDocument> getPhraseHistoryForUser(String userId);
}
