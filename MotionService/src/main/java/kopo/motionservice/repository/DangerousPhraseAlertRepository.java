package kopo.motionservice.repository;

import kopo.motionservice.repository.document.DangerousPhraseAlertDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DangerousPhraseAlertRepository extends MongoRepository<DangerousPhraseAlertDocument, String> {
    List<DangerousPhraseAlertDocument> findByUserIdAndDetectedTimeAfter(String userId, LocalDateTime detectedTime);
}
