package com.example.usermgmt.userservice.dto.common;

import java.util.List;
import lombok.Data;

@Data
public class BatchOperationRequest {

    private List<String> ids;
    private Boolean force;
}
