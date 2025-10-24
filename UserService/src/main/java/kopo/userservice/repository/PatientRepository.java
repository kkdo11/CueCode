package kopo.userservice.repository;

import kopo.userservice.model.PatientDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends MongoRepository<PatientDocument, String> {
    // 커스텀 쿼리 필요시 여기에 작성
    List<PatientDocument> findByManagerIdsContaining(String managerId);
    PatientDocument findByIdAndEmail(String id, String email);
    Optional<PatientDocument> findByEmail(String email);
}
