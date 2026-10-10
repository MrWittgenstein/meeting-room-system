package com.example.usermgmt.notification.controller;

import com.example.usermgmt.common.api.ApiResponse;
import com.example.usermgmt.notification.dto.PolicyDtos;
import com.example.usermgmt.notification.service.ReusableMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final ReusableMessageService reusableMessageService;

    @PostMapping
    public ApiResponse<PolicyDtos.Record> create(@Valid @RequestBody PolicyDtos.CreateRequest request) {
        return ApiResponse.success("Policy created", reusableMessageService.createPolicy(request));
    }

    @GetMapping("/{policyId}")
    public ApiResponse<PolicyDtos.Record> detail(@PathVariable("policyId") String policyId) {
        return ApiResponse.success(reusableMessageService.getPolicy(policyId));
    }

    @PutMapping("/{policyId}")
    public ApiResponse<PolicyDtos.Record> update(
        @PathVariable("policyId") String policyId,
        @Valid @RequestBody PolicyDtos.UpdateRequest request
    ) {
        return ApiResponse.success("Policy updated", reusableMessageService.updatePolicy(policyId, request));
    }

    @DeleteMapping("/{policyId}")
    public ApiResponse<Void> delete(@PathVariable("policyId") String policyId) {
        reusableMessageService.deletePolicy(policyId);
        return ApiResponse.success("Policy deleted", null);
    }

    @GetMapping
    public ApiResponse<PolicyDtos.ListResponse> list(
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "size", defaultValue = "20") Integer size,
        @RequestParam(value = "business_code", required = false) String businessCode,
        @RequestParam(value = "channel_type", required = false) String channelType,
        @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(reusableMessageService.listPolicies(page, size, businessCode, channelType, status));
    }
}
