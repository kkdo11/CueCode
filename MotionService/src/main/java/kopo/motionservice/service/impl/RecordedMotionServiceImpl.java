package kopo.motionservice.service.impl;

import kopo.motionservice.dto.RecordedMotionDTO;
import kopo.motionservice.service.IRecordedMotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordedMotionServiceImpl implements IRecordedMotionService {

    private final MongoTemplate mongoTemplate;
    private static final String COLLECTION_NAME = "RECORDED_MOTIONS";

    @Override
    public List<RecordedMotionDTO> getRecordedMotionsByUserId(String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                log.warn("[RecordedMotionServiceImpl] getRecordedMotionsByUserId called with null/blank userId");
                return List.of();
            }

            log.info("[RecordedMotionServiceImpl] getRecordedMotionsByUserId called with userId: {}", userId);
            log.debug("[RecordedMotionServiceImpl] Using collection: {}", COLLECTION_NAME);

            Query query = new Query(Criteria.where("user_id").is(userId));
            // _id 필드를 포함하여 응답 DTO에 id로 전달하도록 함
            query.fields().include("_id").include("phrase").include("motion_type").include("description");

            // 쿼리 정보를 디버그 레벨로 출력
            try {
                log.debug("[RecordedMotionServiceImpl] QueryObject: {}", query.getQueryObject());
                log.debug("[RecordedMotionServiceImpl] FieldsObject: {}", query.getFieldsObject());
            } catch (Exception ex) {
                // Query#getQueryObject 등은 내부 구현에 따라 다를 수 있으므로 실패해도 진행
                log.debug("[RecordedMotionServiceImpl] Failed to print query/fields object: {}", ex.getMessage());
            }

            List<Document> docs = mongoTemplate.find(query, Document.class, COLLECTION_NAME);

            log.info("[RecordedMotionServiceImpl] Retrieved {} documents for userId: {}", docs.size(), userId);
            if (!docs.isEmpty()) {
                // 첫 몇 개 문서 샘플을 디버그로 출력 (최대 5개)
                docs.stream().limit(5).forEach(d -> log.debug("[RecordedMotionServiceImpl] sample doc: {}", d.toJson()));
            }

            // 중복 제거: phrase + motion_type 조합을 키로 사용하여 첫 번째 항목만 반환
            Map<String, RecordedMotionDTO> uniqueMotions = docs.stream()
                    .map(d -> {
                        // _id를 안전하게 문자열로 변환
                        String id = null;
                        try {
                            id = d.getObjectId("_id").toHexString();
                        } catch (Exception e) {
                            Object idObj = d.get("_id");
                            if (idObj != null) id = idObj.toString();
                        }

                        return new RecordedMotionDTO(
                                id,
                                d.getString("phrase"),
                                d.getString("motion_type"),
                                d.getString("description")
                        );
                    })
                    .collect(Collectors.toMap(
                            dto -> dto.getPhrase() + "_" + dto.getMotionType(),
                            dto -> dto,
                            (existing, replacement) -> existing
                    ));

            List<RecordedMotionDTO> result = new java.util.ArrayList<>(uniqueMotions.values());
            log.info("[RecordedMotionServiceImpl] Returning {} unique motions for userId: {}", result.size(), userId);

            return result;
        } catch (Exception e) {
            log.error("[RecordedMotionServiceImpl] Failed to get recorded motions for userId {}", userId, e);
            return List.of();
        }
    }

    @Override
    public boolean deleteRecordedMotionsByUserId(String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                log.warn("[RecordedMotionServiceImpl] deleteRecordedMotionsByUserId called with null/blank userId");
                return false;
            }

            log.info("[RecordedMotionServiceImpl] deleteRecordedMotionsByUserId called for userId: {}", userId);
            // 먼저 중복되지 않은 고유한 phrase + motion_type 조합 찾기
            Query findQuery = new Query(Criteria.where("user_id").is(userId));
            findQuery.fields().include("phrase").include("motion_type").exclude("_id");

            List<Document> docs = mongoTemplate.find(findQuery, Document.class, COLLECTION_NAME);
            log.info("[RecordedMotionServiceImpl] Found {} documents to evaluate for deletion for userId: {}", docs.size(), userId);

            // 고유한 phrase + motion_type 조합별로 삭제
            long totalDeleted = 0;
            Map<String, Document> uniqueCombinations = docs.stream()
                    .collect(Collectors.toMap(
                            d -> d.getString("phrase") + "_" + d.getString("motion_type"),
                            d -> d,
                            (existing, replacement) -> existing
                    ));

            log.debug("[RecordedMotionServiceImpl] Unique combinations to delete: {}", uniqueCombinations.keySet());

            for (Document doc : uniqueCombinations.values()) {
                Query deleteQuery = new Query(Criteria.where("user_id").is(userId)
                        .and("phrase").is(doc.getString("phrase"))
                        .and("motion_type").is(doc.getString("motion_type")));

                long deletedCount = mongoTemplate.remove(deleteQuery, COLLECTION_NAME).getDeletedCount();
                totalDeleted += deletedCount;

                log.info("[RecordedMotionServiceImpl] Deleted {} documents for userId: {}, phrase: {}, motion_type: {}",
                        deletedCount, userId, doc.getString("phrase"), doc.getString("motion_type"));
            }

            log.info("[RecordedMotionServiceImpl] Total deleted {} recorded motions for userId: {}", totalDeleted, userId);
            return totalDeleted > 0;
        } catch (Exception e) {
            log.error("[RecordedMotionServiceImpl] Failed to delete recorded motions for userId {}", userId, e);
            return false;
        }
    }

    @Override
    public long deleteRecordedMotionById(String motionId, String userId) {
        try {
            if (userId == null || userId.isBlank() || motionId == null || motionId.isBlank()) {
                log.warn("[RecordedMotionServiceImpl] deleteRecordedMotionById called with null/blank motionId or userId");
                return 0;
            }

            log.info("[RecordedMotionServiceImpl] deleteRecordedMotionById called for motionId: {}, userId: {}", motionId, userId);

            // 1) 먼저 주어진 motionId로 문서를 조회하여 phrase와 motion_type을 얻는다.
            Object idValue;
            try {
                idValue = new org.bson.types.ObjectId(motionId);
            } catch (Exception e) {
                idValue = motionId;
            }

            Query findQuery = new Query(Criteria.where("_id").is(idValue).and("user_id").is(userId));
            Document found = mongoTemplate.findOne(findQuery, Document.class, COLLECTION_NAME);
            if (found == null) {
                log.info("[RecordedMotionServiceImpl] No document found for motionId: {} and userId: {}", motionId, userId);
                return 0;
            }

            String phrase = found.getString("phrase");
            String motionType = found.getString("motion_type");
            if (phrase == null || motionType == null) {
                log.warn("[RecordedMotionServiceImpl] Found document missing phrase or motion_type for _id: {}", motionId);
                return 0;
            }

            // 2) 안전하게 삭제: 사용자(user_id)의 모든 문서를 불러와 Java 레벨에서 정규화(공백 제거/소문자)하여 비교,
            //    매칭되는 문서들의 _id 목록을 취합 후 한 번에 삭제합니다. 이렇게 하면 필드 저장 방식(유니코드/스페이싱 등)의 미세 차이로 삭제가 누락되는 것을 방지합니다.
            Query allUserDocsQuery = new Query(Criteria.where("user_id").is(userId));
            List<Document> userDocs = mongoTemplate.find(allUserDocsQuery, Document.class, COLLECTION_NAME);

            String normalizedTargetPhrase = normalizeString(phrase);
            String normalizedTargetMotionType = normalizeString(motionType);

            // 분리: ObjectId와 문자열 ID를 각각 수집
            List<org.bson.types.ObjectId> oidList = new java.util.ArrayList<>();
            List<String> sidList = new java.util.ArrayList<>();

            for (Document d : userDocs) {
                String p = d.getString("phrase");
                String m = d.getString("motion_type");
                if (p == null || m == null) continue;
                if (!normalizedTargetPhrase.equals(normalizeString(p)) || !normalizedTargetMotionType.equals(normalizeString(m))) continue;

                // try get ObjectId first
                try {
                    org.bson.types.ObjectId oid = d.getObjectId("_id");
                    if (oid != null) oidList.add(oid);
                    else {
                        Object idObj = d.get("_id");
                        if (idObj != null) sidList.add(idObj.toString());
                    }
                } catch (Exception ex) {
                    Object idObj = d.get("_id");
                    if (idObj != null) sidList.add(idObj.toString());
                }
            }

            log.info("[RecordedMotionServiceImpl] Identified {} ObjectId(s) and {} String-id(s) to delete for userId={}, phrase='{}', motion_type='{}'.",
                    oidList.size(), sidList.size(), userId, phrase, motionType);

            long deleted = 0;
            if (!oidList.isEmpty()) {
                Query q = new Query(Criteria.where("_id").in(oidList));
                deleted += mongoTemplate.remove(q, COLLECTION_NAME).getDeletedCount();
            }
            if (!sidList.isEmpty()) {
                Query q2 = new Query(Criteria.where("_id").in(sidList));
                deleted += mongoTemplate.remove(q2, COLLECTION_NAME).getDeletedCount();
            }

            log.info("[RecordedMotionServiceImpl] deleteRecordedMotionById removed {} documents for userId: {}, phrase: {}, motion_type: {}",
                    deleted, userId, phrase, motionType);

            return deleted;
        } catch (Exception e) {
            log.error("[RecordedMotionServiceImpl] Failed to delete recorded motion {} for userId {}", motionId, userId, e);
            return 0;
        }
    }

    // helper: normalize by trimming, converting to lower case, and collapsing whitespace
    private String normalizeString(String s) {
        if (s == null) return null;
        return s.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
