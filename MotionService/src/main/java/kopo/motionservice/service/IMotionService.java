package kopo.motionservice.service;

import kopo.motionservice.dto.MotionRecordRequestDTO;
import kopo.motionservice.repository.document.RecordedMotionDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IMotionService {

    /**
     * 녹화된 동작 데이터를 DB에 저장하는 메서드
     * @param requestDTO 프론트엔드로부터 받은 동작 데이터 묶음
     */
    void saveRecordedMotion(MotionRecordRequestDTO requestDTO);

//    /**
//     * 프레이즈, 검출영역, 영상 파일을 FastAPI로 전송하는 메서드
//     * @param phrase 사용자 입력 프레이즈
//     * @param detectionArea 검출 영역
//     * @param videoFile 사용자 업로드 영상 파일
//     * @return FastAPI 응답 결과
//     */
//    String sendMotionVideoToFastAPI(String phrase, String detectionArea, org.springframework.web.multipart.MultipartFile videoFile);
//
//    /**
//     * 프레이즈, 검출영역, 영상 파일, 트리밍 구간을 FastAPI로 전송하는 메서드
//     * @param phrase 사용자 입력 프레이즈
//     * @param detectionArea 검출 영역
//     * @param videoFile 사용자 업로드 영상 파일
//     * @param trimStart 영상 시작 트리밍 구간 (초)
//     * @param trimEnd 영상 종료 트리밍 구간 (초)
//     * @return FastAPI 응답 결과
//     */
//    String sendMotionVideoToFastAPI(String phrase, String detectionArea, org.springframework.web.multipart.MultipartFile videoFile, String trimStart, String trimEnd);
//
//    /**
//     * 프레이즈, 검출영역, 영상 파일, 사용자 ID를 FastAPI로 전송하는 메서드
//     * @param phrase 사용자 입력 프레이즈
//     * @param detectionArea 검출 영역
//     * @param videoFile 사용자 업로드 영상 파일
//     * @param userId 동작을 저장할 사용자 ID
//     * @return FastAPI 응답 결과
//     */
//    String sendMotionVideoToFastAPI(String phrase, String detectionArea, org.springframework.web.multipart.MultipartFile videoFile, String userId);
//
//    /**
//     * 프레이즈, 검출영역, 영상 파일, 트리밍 구간, 사용자 ID를 FastAPI로 전송하는 메서드
//     * @param phrase 사용자 입력 프레이즈
//     * @param detectionArea 검출 영역
//     * @param videoFile 사용자 업로드 영상 파일
//     * @param trimStart 영상 시작 트리밍 구간 (초)
//     * @param trimEnd 영상 종료 트리밍 구간 (초)
//     * @param userId 동작을 저장할 사용자 ID
//     * @return FastAPI 응답 결과
//     */
//    String sendMotionVideoToFastAPI(String phrase, String detectionArea, org.springframework.web.multipart.MultipartFile videoFile, String trimStart, String trimEnd, String userId);
//
//    // New: single file + description
//    String sendMotionVideoToFastAPI(
//            String phrase,
//            String detectionArea,
//            org.springframework.web.multipart.MultipartFile videoFile,
//            String userId,
//            String description
//    );
//
//    // New: with trim + description
//    String sendMotionVideoToFastAPI(
//            String phrase,
//            String detectionArea,
//            org.springframework.web.multipart.MultipartFile videoFile,
//            String trimStart,
//            String trimEnd,
//            String userId,
//            String description
//    );

    /**
     * Basic single-file upload with optional description.
     *
     * @param phrase         motion label (user-defined meaning)
     * @param detectionArea  area such as hands / face / eyes
     * @param videoFile      uploaded motion video
     * @param userId         current user id
     * @param description    optional free-text description
     * @return FastAPI response JSON or message
     */
    String sendMotionVideoToFastAPI(
            String phrase,
            String detectionArea,
            MultipartFile videoFile,
            String userId,
            String description
    );

    /**
     * Advanced form with trimming parameters and optional description.
     *
     * @param phrase         motion label (user-defined meaning)
     * @param detectionArea  area such as hands / face / eyes
     * @param videoFile      uploaded motion video
     * @param userId         current user id
     * @param trimStart      optional trim start (sec)
     * @param trimEnd        optional trim end (sec)
     * @param description    optional free-text description
     * @return FastAPI response JSON or message
     */
    String sendMotionVideoToFastAPI(
            String phrase,
            String detectionArea,
            MultipartFile videoFile,
            String userId,
            String trimStart,
            String trimEnd,
            String description
    );


    /**
     * Returns all recorded motions from storage (used by matching service)
     */
    List<RecordedMotionDocument> getAllRecordedMotions();

    public String generateSentence(String userId);
}
