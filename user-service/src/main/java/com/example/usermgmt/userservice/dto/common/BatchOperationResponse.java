package com.example.usermgmt.userservice.dto.common;

import java.util.List;
import lombok.Data;

@Data
public class BatchOperationResponse {

    private Integer successCount;
    private Integer failCount;
    private List<BatchFailItemResponse> failList;
}
