package kopo.motionservice.repository.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Setter;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "RECORDED_MOTIONS")
public class RecordedMotionDocument {

    @Id
    private String recordId;

    @Field(name = "user_id")
    private String userId;

    @Indexed
    @Field(name = "phrase")
    private String phrase;

    @Field(name = "motion_type")
    private String motionType;

    @Field(name = "motion_data")
    private MotionDataDocument motionData;

    @Field(name = "description")
    private String description;

    @Field(name = "created_at")
    private Date createdAt;

    @Builder
    public RecordedMotionDocument(String userId, String phrase, String motionType, MotionDataDocument motionData, String description) {
        this.userId = userId;
        this.phrase = phrase;
        this.motionType = motionType;
        this.motionData = motionData;
        this.description = description;
        this.createdAt = new Date();
    }

    // Nested class for motion_data
    @Getter
    @Setter
    @NoArgsConstructor
    public static class MotionDataDocument {
        @Field(name = "face_blendshapes")
        private List<FaceBlendshapesFrameDocument> faceBlendshapes;

        @Field(name = "hand_landmarks")
        private List<HandLandmarksFrameDocument> handLandmarks;

        @Field(name = "eye_landmarks")
        private List<EyeLandmarksFrameDocument> eyeLandmarks;
    }

    // Nested class for face_blendshapes frames
    @Getter
    @Setter
    @NoArgsConstructor
    public static class FaceBlendshapesFrameDocument {
        @Field(name = "timestamp_ms")
        private long timestampMs;
        private Map<String, Double> values;
    }

    // Nested class for hand_landmarks frames
    @Getter
    @Setter
    @NoArgsConstructor
    public static class HandLandmarksFrameDocument {
        @Field(name = "timestamp_ms")
        private long timestampMs;

        @Field(name = "right_hand")
        private List<List<Double>> rightHand;

        @Field(name = "left_hand")
        private List<List<Double>> leftHand;
    }

    // Nested class for eye_landmarks frames
    @Getter
    @Setter
    @NoArgsConstructor
    public static class EyeLandmarksFrameDocument {
        @Field(name = "timestamp_ms")
        private long timestampMs;

        @Field(name = "right_eye")
        private List<List<Double>> rightEye;

        @Field(name = "left_eye")
        private List<List<Double>> leftEye;
    }


}
