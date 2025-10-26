package kopo.motionservice.service;

import kopo.motionservice.repository.document.DangerousPhraseAlertDocument;

import java.util.List;
import java.util.Optional;

public interface IDangerousPhraseAlertService {
    List<DangerousPhraseAlertDocument> getDangerousPhraseAlertsForUser(String userId);

    Optional<DangerousPhraseAlertDocument> getLatestAlertForManager(String managerId);

    void confirmAlert(String alertId);
}
