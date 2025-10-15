package kopo.userservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoDTO {
    private String id;
    private String email;
    private String name;
    private String userType; // "patient" 또는 "manager"
}

