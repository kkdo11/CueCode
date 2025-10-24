package kopo.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record MsgDTO(
        int result //결과 코드
, String msg //결과 메시지
, String accessToken // JWT accessToken 추가
, String userName // 사용자 이름 추가
) {
}
