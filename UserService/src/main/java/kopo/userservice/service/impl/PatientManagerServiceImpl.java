package kopo.userservice.service.impl;

import kopo.userservice.dto.MailDTO;
import kopo.userservice.model.PatientDocument;
import kopo.userservice.model.ManagerDocument;
import kopo.userservice.repository.PatientRepository;
import kopo.userservice.repository.ManagerRepository;
import kopo.userservice.service.IMailService;
import kopo.userservice.service.PatientManagerService;
import kopo.userservice.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientManagerServiceImpl implements PatientManagerService {

    private final PatientRepository patientRepository;
    private final ManagerRepository managerRepository;
    private final IMailService mailService;

    /** 보호자가 환자를 관리 목록에 추가 (양방향 관계 업데이트) */
    @Transactional
    public boolean addPatientToManager(String managerId, String patientId) {
        Optional<PatientDocument> patientOpt = patientRepository.findById(patientId);
        Optional<ManagerDocument> managerOpt = managerRepository.findById(managerId);
        if (patientOpt.isEmpty() || managerOpt.isEmpty()) return false;

        PatientDocument patient = patientOpt.get();
        ManagerDocument manager = managerOpt.get();

        // 환자 문서에 보호자 ID 추가
        List<String> managerIds = patient.getManagerIds();
        if (managerIds == null) managerIds = new ArrayList<>();
        if (!managerIds.contains(managerId)) managerIds.add(managerId);
        patient.setManagerIds(managerIds);
        patientRepository.save(patient);

        // 보호자 문서에 환자 ID 추가
        List<String> patientIds = manager.getPatientIds();
        if (patientIds == null) patientIds = new ArrayList<>();
        if (!patientIds.contains(patientId)) patientIds.add(patientId);
        manager.setPatientIds(patientIds);
        managerRepository.save(manager);

        // 이메일 발송 로직 추가
        sendEmailNotification(patient, manager, "add");

        return true;
    }

    /** 환자와 보호자의 연결 해제 (양방향 관계 업데이트) */
    @Transactional
    @Override
    public void unlinkManagerFromPatient(String patientId, String managerId) {
        // 환자 정보 조회 및 업데이트
        Optional<PatientDocument> patientOpt = patientRepository.findById(patientId);
        patientOpt.ifPresent(patient -> {
            List<String> managerIds = patient.getManagerIds();
            if (managerIds != null) {
                managerIds.remove(managerId);
                patient.setManagerIds(managerIds);
                patientRepository.save(patient);
            }
        });

        // 매니저 정보 조회 및 업데이트
        Optional<ManagerDocument> managerOpt = managerRepository.findById(managerId);
        managerOpt.ifPresent(manager -> {
            List<String> patientIds = manager.getPatientIds();
            if (patientIds != null) {
                patientIds.remove(patientId);
                manager.setPatientIds(patientIds);
                managerRepository.save(manager);
            }

            // 이메일 발송 로직 추가
            patientOpt.ifPresent(patient -> sendEmailNotification(patient, manager, "unlink"));
        });
    }

    /**
     * 환자에게 매니저 추가/삭제 알림 이메일을 발송하는 헬퍼 메서드
     * @param patient 알림을 받을 환자
     * @param manager 관련된 매니저
     * @param type 작업 유형 ("add" 또는 "unlink")
     */
    private void sendEmailNotification(PatientDocument patient, ManagerDocument manager, String type) {
        if (patient.getEmail() == null || patient.getEmail().isEmpty()) {
            log.warn("환자 ID '{}'의 이메일 주소가 없어 알림을 보낼 수 없습니다.", patient.getId());
            return;
        }

        try {
            String toMail = EncryptUtil.decAES128CBC(patient.getEmail());
            String title;
            String content;

            if ("add".equals(type)) {
                title = "[CueCode] 보호자(매니저)가 추가되었습니다.";
                content = buildEmailTemplate(patient.getName(), manager.getName(),
                        "새로운 보호자로 등록되었습니다.",
                        "이제부터 <strong>" + manager.getName() + "</strong> 님이 회원님의 건강 상태 및 활동을 모니터링할 수 있습니다.",
                        "#1976d2");
            } else { // "unlink"
                title = "[CueCode] 보호자(매니저)와의 연결이 해제되었습니다.";
                content = buildEmailTemplate(patient.getName(), manager.getName(),
                        "보호자 연결이 해제되었습니다.",
                        "<strong>" + manager.getName() + "</strong> 님은 더 이상 회원님의 정보를 확인할 수 없습니다.",
                        "#d32f2f");
            }

            MailDTO mailDTO = new MailDTO();
            mailDTO.setToMail(toMail);
            mailDTO.setTitle(title);
            mailDTO.setContents(content);

            mailService.doSendMail(mailDTO);
            log.info("환자 '{}'에게 '{}' 유형의 알림 이메일을 성공적으로 발송했습니다.", patient.getId(), type);

        } catch (Exception e) {
            log.error("환자 ID '{}'에게 알림 이메일 발송 중 오류가 발생했습니다: {}", patient.getId(), e.getMessage());
        }
    }

    private String buildEmailTemplate(String patientName, String managerName, String header, String message, String accentColor) {
        return "<div style='max-width:600px; margin:20px auto; padding:40px; font-family:Arial, sans-serif; border:1px solid #e0e0e0; border-radius:12px; box-shadow:0 4px 12px rgba(0,0,0,0.1);'>"
                + "    <div style='text-align:center; border-bottom: 2px solid " + accentColor + "; padding-bottom: 20px; margin-bottom: 20px;'>"
                + "        <h1 style='color:" + accentColor + "; margin:0; font-size:28px;'>CueCode 알림</h1>"
                + "    </div>"
                + "    <div style='padding: 0 20px;'>"
                + "        <p style='font-size:18px; color:#333;'>안녕하세요, <strong>" + patientName + "</strong> 님.</p>"
                + "        <p style='font-size:16px; color:#555; line-height:1.6;'>" + header + "</p>"
                + "        <div style='background-color:#f9f9f9; border-left: 5px solid " + accentColor + "; padding: 20px; margin: 30px 0; border-radius: 5px;'>"
                + "            <p style='margin:0; font-size:16px; color:#333;'>" + message + "</p>"
                + "        </div>"
                + "        <p style='font-size:14px; color:#777;'>관련된 보호자(매니저): <strong>" + managerName + "</strong></p>"
                + "    </div>"
                + "    <div style='text-align:center; margin-top:40px; padding-top: 20px; border-top: 1px solid #eee;'>"
                + "        <p style='font-size:12px; color:#aaa;'>본 메일은 발신전용입니다. 문의사항은 CueCode 홈페이지를 통해 접수해주세요.<br/>&copy; CueCode Team</p>"
                + "    </div>"
                + "</div>";
    }
}
