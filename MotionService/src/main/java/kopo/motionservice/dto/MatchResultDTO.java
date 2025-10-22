package kopo.motionservice.dto;

import lombok.Data;

@Data
public class MatchResultDTO {
    private String recordId;
    private String phrase;
    private String motionType;
    private double score;

    public MatchResultDTO(String recordId, String phrase, String motionType, double score) {
        this.recordId = recordId;
        this.phrase = phrase;
        this.motionType = motionType;
        this.score = score;
    }

    public static MatchResultDTO noMatch() {
        return new MatchResultDTO(null, null, null, Double.POSITIVE_INFINITY);
    }
}
