package kopo.userservice.controller;

import kopo.userservice.dto.PatientDTO;
import kopo.userservice.model.PatientDocument;
import kopo.userservice.repository.PatientRepository;
import kopo.userservice.service.PatientManagerService;
import kopo.userservice.util.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import kopo.userservice.repository.DetectionAreaRepository;
import kopo.userservice.repository.document.DetectionAreaDocument;

@Slf4j
@RestController
@RequestMapping("/patient")
public class PatientController {
    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientManagerService patientManagerService;

    @Autowired
    private DetectionAreaRepository detectionAreaRepository;

    @GetMapping("/list")
    public List<PatientDocument> getPatientsByManager(@RequestParam String managerId) {
        return patientRepository.findByManagerIdsContaining(managerId);
    }

    @GetMapping("/detail")
    public PatientDTO getPatientDetail(@RequestParam String id) {
        log.info("[PatientController] /patient/detail?id={} called", id);
        Optional<PatientDocument> patientOptional = patientRepository.findById(id);
        if (patientOptional.isEmpty()) {
            return null;
        }

        PatientDocument patient = patientOptional.get();
        String decryptedEmail = "";
        try {
            if (patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                decryptedEmail = EncryptUtil.decAES128CBC(patient.getEmail());
            }
        } catch (Exception e) {
            log.error("Failed to decrypt email for patient id: {}", id, e);
            decryptedEmail = "복호화 오류"; // 복호화 실패 시 표시할 메시지
        }

        return PatientDTO.builder()
                .id(patient.getId())
                .name(patient.getName())
                .email(decryptedEmail)
                .managerIds(patient.getManagerIds())
                .medicalHistory(patient.getMedicalHistory())
                .medications(patient.getMedications())
                .allergies(patient.getAllergies())
                .build();
    }

    @PostMapping("/update")
    public ResponseEntity<?> updatePatient(@RequestBody PatientDocument req) {
        Optional<PatientDocument> optional = patientRepository.findById(req.getId());
        if (optional.isEmpty()) {
            return ResponseEntity.status(404).body("환자를 찾을 수 없습니다.");
        }
        PatientDocument patient = optional.get();
        // if (req.getName() != null) patient.setName(req.getName()); // 이름은 수정 불가능
        if (req.getMedicalHistory() != null) patient.setMedicalHistory(req.getMedicalHistory());
        if (req.getMedications() != null) patient.setMedications(req.getMedications());
        if (req.getAllergies() != null) patient.setAllergies(req.getAllergies());
        // 필요시 다른 필드도 추가
        patientRepository.save(patient);
        return ResponseEntity.ok("수정 완료");
    }

    @PostMapping("/unlink-manager")
    public ResponseEntity<?> unlinkManager(@RequestBody Map<String, String> request, Principal principal) {
        String patientId = request.get("patientId");
        String managerId = principal.getName(); // JWT 토큰에서 managerId 추출

        if (patientId == null || patientId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "환자 ID가 필요합니다."));
        }

        try {
            patientManagerService.unlinkManagerFromPatient(patientId, managerId);
            return ResponseEntity.ok(Map.of("message", "환자와의 연결이 성공적으로 해제되었습니다."));
        } catch (Exception e) {
            log.error("Error unlinking manager {} from patient {}: {}", managerId, patientId, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "연결 해제 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/detection-area/read")
    public ResponseEntity<?> postDetectionAreaRead(@RequestBody Map<String, String> req) {
        log.info("[백엔드] 감지 범위 조회 API 진입, 요청 body: {}", req);
        String patientId = req.get("patientId");
        log.info("[백엔드] 감지 범위 조회 요청 patientId={}", patientId);
        // 환자 존재 여부 확인
        boolean patientExists = patientRepository.findById(patientId).isPresent();
        log.info("[백엔드] 환자 존재 여부: {}", patientExists);
        if (!patientExists) {
            log.warn("[백엔드] 감지 범위 조회: 존재하지 않는 환자 patientId={}", patientId);
            return ResponseEntity.ok(Map.of("result", 0, "msg", "존재하지 않는 환자입니다."));
        }
        Optional<DetectionAreaDocument> areaOpt = detectionAreaRepository.findById(patientId);
        log.info("[백엔드] 감지 범위 DB 조회 결과 areaOpt.isPresent={}", areaOpt.isPresent());
        if (areaOpt.isPresent()) {
            DetectionAreaDocument doc = areaOpt.get();
            log.info("[백엔드] 감지 범위 도큐먼트: {}", doc);
            String type = doc.isHand() ? "hand" : doc.isFace() ? "face" : doc.isBoth() ? "both" : "none";
            log.info("[백엔드] 감지 범위 응답: type={}, hand={}, face={}, both={}", type, doc.isHand(), doc.isFace(), doc.isBoth());
            return ResponseEntity.ok(Map.of(
                "result", 1,
                "patientId", doc.getPatientId(),
                "detectionArea", type,
                "hand", doc.isHand(),
                "face", doc.isFace(),
                "both", doc.isBoth()
            ));
        } else {
            log.warn("[백엔드] 감지 범위 정보 없음, patientId={}", patientId);
            return ResponseEntity.ok(Map.of("result", 0, "msg", "감지 범위 정보 없음"));
        }
    }

    @PostMapping("/detection-area/update")
    public ResponseEntity<?> postDetectionAreaUpdate(@RequestBody Map<String, String> req) {
        String patientId = req.get("patientId");
        String detectionAreaType = req.get("detectionAreaType");
        if (patientId == null || patientId.isBlank()) {
            log.warn("[POST] 감지 범위 변경: patientId가 null 또는 빈 문자열입니다. 요청 거부");
            return ResponseEntity.badRequest().body(Map.of("result", 0, "msg", "유효하지 않은 환자 ID입니다."));
        }
        if (!"hand".equals(detectionAreaType) && !"face".equals(detectionAreaType) && !"both".equals(detectionAreaType)) {
            log.warn("[POST] 감지 범위 변경: detectionAreaType 값이 잘못됨: {}", detectionAreaType);
            return ResponseEntity.badRequest().body(Map.of("result", 0, "msg", "유효하지 않은 감지 범위 타입입니다."));
        }
        log.info("[POST] 감지 범위 변경 요청 patientId={}, type={}", patientId, detectionAreaType);
        try {
            Optional<DetectionAreaDocument> areaOpt = detectionAreaRepository.findByPatientId(patientId);
            DetectionAreaDocument doc = areaOpt.orElse(DetectionAreaDocument.builder()
                .patientId(patientId).hand(false).face(false).both(false).build());
            doc = DetectionAreaDocument.builder()
                .patientId(patientId)
                .hand("hand".equals(detectionAreaType))
                .face("face".equals(detectionAreaType))
                .both("both".equals(detectionAreaType))
                .build();
            detectionAreaRepository.save(doc);
            return ResponseEntity.ok(Map.of("result", 1, "msg", "감지 범위가 변경되었습니다."));
        } catch (Exception e) {
            log.error("[POST] 감지 범위 변경 중 예외 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("result", 0, "msg", "서버 오류: " + e.getMessage()));
        }
    }
}
