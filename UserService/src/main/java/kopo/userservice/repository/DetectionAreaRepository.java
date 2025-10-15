package kopo.userservice.repository;

import kopo.userservice.repository.document.DetectionAreaDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DetectionAreaRepository extends MongoRepository<DetectionAreaDocument, String> {
    Optional<DetectionAreaDocument> findByPatientId(String patientId);
}
