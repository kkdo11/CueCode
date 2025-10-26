package kopo.motionservice.repository;

import kopo.motionservice.repository.document.DangerousPhraseAlertDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DangerousPhraseAlertRepository extends MongoRepository<DangerousPhraseAlertDocument, String> {
    List<DangerousPhraseAlertDocument> findByUserIdAndDetectedTimeAfter(String userId, LocalDateTime detectedTime);

    Optional<DangerousPhraseAlertDocument> findTopByUserIdInAndConfirmedIsFalseOrderByDetectedTimeDesc(List<String> userIds);
}
