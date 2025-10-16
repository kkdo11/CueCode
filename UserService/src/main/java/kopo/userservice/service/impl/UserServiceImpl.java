//package kopo.userservice.service.impl;
//
//import kopo.userservice.dto.PatientDTO;
//import kopo.userservice.dto.ManagerDTO;
//import kopo.userservice.service.IUserService;
//import kopo.userservice.model.Patient;
//import kopo.userservice.model.Manager;
//import kopo.userservice.repository.PatientRepository;
//import kopo.userservice.repository.ManagerRepository;
//import kopo.userservice.util.EncryptUtil;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Service
//public class UserServiceImpl implements IUserService {
//    @Autowired
//    private PatientRepository patientRepository;
//    @Autowired
//    private ManagerRepository managerRepository;
//
//    @Override
//    public int insertPatient(PatientDTO dto) {
//        // TODO: 실제 구현
//        return 0;
//    }
//    @Override
//    public int insertManager(ManagerDTO dto) {
//        // TODO: 실제 구현
//        return 0;
//    }
//    @Override
//    public PatientDTO getPatient(PatientDTO dto) throws Exception {
//        // TODO: 실제 구현
//        return null;
//    }
//    @Override
//    public ManagerDTO getManager(ManagerDTO dto) throws Exception {
//        // TODO: 실제 구현
//        return null;
//    }
//    @Override
//    public Object login(String userId, String password) {
//        // TODO: 실제 구현
//        return null;
//    }
//    @Override
//    public boolean verifyPassword(String userId, String password) {
//        // 환자 먼저 조회
//        Patient patient = patientRepository.findById(userId).orElse(null);
//        if (patient != null) {
//            String encPw = EncryptUtil.encHashSHA256(password);
//            return encPw.equals(patient.getPassword());
//        }
//        // 관리자 조회
//        Manager manager = managerRepository.findById(userId).orElse(null);
//        if (manager != null) {
//            String encPw = EncryptUtil.encHashSHA256(password);
//            return encPw.equals(manager.getPassword());
//        }
//        return false;
//    }
//    public UserInfoDTO getUserInfo(String userId) {
//        ManagerDocument manager = managerRepository.findById(userId).orElse(null);
//        if (manager == null) return null;
//        return UserInfoDTO.builder()
//                .id(manager.getId())
//                .name(manager.getName())
//                .email(manager.getEmail())
//                .build();
//    }
//    public boolean updateName(String userId, String newName) {
//        ManagerDocument manager = managerRepository.findById(userId).orElse(null);
//        if (manager == null) return false;
//        manager.setName(newName);
//        managerRepository.save(manager);
//        return true;
//    }
//    public boolean updateEmail(String userId, String newEmail) {
//        ManagerDocument manager = managerRepository.findById(userId).orElse(null);
//        if (manager == null) return false;
//        manager.setEmail(newEmail);
//        managerRepository.save(manager);
//        return true;
//    }
//    public boolean updateId(String userId, String newId) {
//        ManagerDocument manager = managerRepository.findById(userId).orElse(null);
//        if (manager == null) return false;
//        managerRepository.deleteById(userId);
//        manager.setId(newId);
//        managerRepository.save(manager);
//        return true;
//    }
//    public boolean updatePassword(String userId, String currentPassword, String newPassword) {
//        ManagerDocument manager = managerRepository.findById(userId).orElse(null);
//        if (manager == null) return false;
//        if (!manager.getPw().equals(currentPassword)) return false;
//        manager.setPw(newPassword);
//        managerRepository.save(manager);
//        return true;
//    }
//}
