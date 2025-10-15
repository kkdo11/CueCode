package kopo.userservice.repository.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "detection_area")
public class DetectionAreaDocument {
    @Id
    private String patientId; // patientId가 MongoDB의 _id로 저장됨
    private boolean hand;
    private boolean face;
    private boolean both;
}
