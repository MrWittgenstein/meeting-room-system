package com.example.usermgmt.userservice.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchFailItemResponse {

    private String userId;
    private String groupId;
    private String reason;

    public static BatchFailItemResponse user(String userId, String reason) {
        return new BatchFailItemResponse(userId, null, reason);
    }

    public static BatchFailItemResponse group(String groupId, String reason) {
        return new BatchFailItemResponse(null, groupId, reason);
    }
}
