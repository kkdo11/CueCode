package kopo.userservice.service;

import kopo.userservice.dto.PatientDTO;
import kopo.userservice.dto.ManagerDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface IUserService {
    // 환자 회원가입
    int insertPatient(PatientDTO dto);
    // 관리자 회원가입
    int insertManager(ManagerDTO dto);
    // 환자 정보 조회
    PatientDTO getPatient(PatientDTO dto) throws Exception;
    // 관리자 정보 조회
    ManagerDTO getManager(ManagerDTO dto) throws Exception;
    /**
     * 로그인: user_id, password로 인증
     * @return 인증된 사용자(PatientDTO 또는 ManagerDTO), 실패 시 null
     */
    Object login(String userId, String password);
    /**
     * 아이디 중복 여부 확인
     */
    boolean existsUserId(String userId);
    // 이메일 인증 코드 전송
    int sendEmailAuthCode(String email) throws Exception;
    /**
     * 로그아웃 시 Access/Refresh Token 블랙리스트 등록
     */
    void invalidateRefreshToken(HttpServletRequest request);
}
