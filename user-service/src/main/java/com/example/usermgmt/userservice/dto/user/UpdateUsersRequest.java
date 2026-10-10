package com.example.usermgmt.userservice.dto.user;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class UpdateUsersRequest {

    @NotEmpty
    private List<String> userIds;

    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String role;
    private Integer status;
    private String remark;
}
