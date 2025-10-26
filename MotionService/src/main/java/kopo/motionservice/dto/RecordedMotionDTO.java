package kopo.motionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordedMotionDTO {
    // MongoDB 문서의 식별자(_id)를 프론트에서 사용하기 위해 id 필드 추가
    private String id;
    private String phrase;
    private String motionType;
    private String description;
}
