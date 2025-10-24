package kopo.motionservice.repository.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "dangerousPhraseAlerts")
public class DangerousPhraseAlertDocument {

    @Id
    private String id;
    private String userId;
    private String phrase;
    private LocalDateTime detectedTime;

    public DangerousPhraseAlertDocument(String userId, String phrase, LocalDateTime detectedTime) {
        this.userId = userId;
        this.phrase = phrase;
        this.detectedTime = detectedTime;
    }
}
