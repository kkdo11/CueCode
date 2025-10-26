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
    private String userName;
    private String phrase;
    private LocalDateTime detectedTime;
    private boolean confirmed = false;

    public DangerousPhraseAlertDocument(String userId, String userName, String phrase, LocalDateTime detectedTime) {
        this.userId = userId;
        this.userName = userName;
        this.phrase = phrase;
        this.detectedTime = detectedTime;
    }
}
