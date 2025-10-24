//package kopo.motionservice.repository;
//
//import kopo.motionservice.repository.document.RecordedMotionDocument;
//import org.springframework.data.mongodb.repository.MongoRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface RecordedMotionRepository extends MongoRepository<RecordedMotionDocument, String> {
//}

// java
package kopo.motionservice.repository;

import kopo.motionservice.repository.document.RecordedMotionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RecordedMotionRepository extends MongoRepository<RecordedMotionDocument, String> {
    // RecordedMotionDocument에 userId 프로퍼티(getter/setter)가 있다면 이 파생 쿼리가 동작합니다.
    List<RecordedMotionDocument> findAllByUserId(String userId);
}