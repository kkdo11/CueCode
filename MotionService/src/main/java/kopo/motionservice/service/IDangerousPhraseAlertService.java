package kopo.motionservice.service;

import kopo.motionservice.repository.document.DangerousPhraseAlertDocument;

import java.util.List;

public interface IDangerousPhraseAlertService {
    List<DangerousPhraseAlertDocument> getDangerousPhraseAlertsForUser(String userId);
    void refreshDangerousPhraseAlertCache(String userId);
}
