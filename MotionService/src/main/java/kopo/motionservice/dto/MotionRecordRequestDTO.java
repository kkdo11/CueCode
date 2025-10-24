package kopo.motionservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MotionRecordRequestDTO {

    // "hello" 등 사용자가 입력한 문구 (label에서 phrase로 변경)
    private String phrase;

    @JsonProperty("motion_type")
    private String motionType; // "face_and_hand"

    @JsonProperty("motion_data")
    private MotionDataDTO motionData;

    @JsonProperty("description")
    private String description;

    @JsonProperty("user_id")
    private String userId; // User ID for identifying the owner of the motion record

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MotionDataDTO {
        @JsonProperty("face_blendshapes")
        private List<FaceBlendshapesFrameDTO> faceBlendshapes;

        @JsonProperty("hand_landmarks")
        private List<HandLandmarksFrameDTO> handLandmarks;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FaceBlendshapesFrameDTO {
        @JsonProperty("timestamp_ms")
        private long timestampMs;
        private Map<String, Double> values;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HandLandmarksFrameDTO {
        @JsonProperty("timestamp_ms")
        private long timestampMs;

        @JsonProperty("right_hand")
        private List<List<Double>> rightHand;

        @JsonProperty("left_hand")
        private List<List<Double>> leftHand;
    }
}