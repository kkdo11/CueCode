package kopo.motionservice.repository;

import kopo.motionservice.repository.document.PhraseHistoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhraseHistoryRepository extends MongoRepository<PhraseHistoryDocument, String> {
    List<PhraseHistoryDocument> findByUserIdAndDetectedTimeAfter(String userId, LocalDateTime detectedTime);

    Optional<PhraseHistoryDocument> findTopByUserIdOrderByDetectedTimeDesc(String userId);
}
