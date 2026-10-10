package com.example.usermgmt.userservice.dto.user;

import java.util.List;
import lombok.Data;

@Data
public class BatchUpdateUsersResponse {

    private Integer successCount;
    private Integer failCount;
    private List<UserDetailResponse> successList;
    private List<FailItemResponse> failList;
}
