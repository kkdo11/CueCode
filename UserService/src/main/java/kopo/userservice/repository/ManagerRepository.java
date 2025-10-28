package kopo.userservice.repository;

import kopo.userservice.model.Manager;
import kopo.userservice.model.ManagerDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ManagerRepository extends MongoRepository<ManagerDocument, String> {
    // 커스텀 쿼리 필요시 여기에 작성
    ManagerDocument findByIdAndEmail(String id, String email);
    List<ManagerDocument> findByEmail(String email);
}
