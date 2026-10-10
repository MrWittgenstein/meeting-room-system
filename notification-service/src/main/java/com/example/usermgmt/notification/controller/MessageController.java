package com.example.usermgmt.notification.controller;

import com.example.usermgmt.common.api.ApiResponse;
import com.example.usermgmt.notification.dto.MessageDtos;
import com.example.usermgmt.notification.service.ReusableMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final ReusableMessageService reusableMessageService;

    @PostMapping("/send")
    public ApiResponse<MessageDtos.SendResponse> send(@Valid @RequestBody MessageDtos.SendRequest request) {
        return ApiResponse.success("Message accepted", reusableMessageService.sendMessage(request));
    }

    @PostMapping("/batch")
    public ApiResponse<MessageDtos.BatchSendResponse> batchSend(@Valid @RequestBody MessageDtos.BatchSendRequest request) {
        return ApiResponse.success("Batch accepted", reusableMessageService.sendBatch(request));
    }

    @DeleteMapping("/{messageId}")
    public ApiResponse<Void> cancel(@PathVariable("messageId") String messageId) {
        reusableMessageService.cancelMessage(messageId);
        return ApiResponse.success("Message canceled", null);
    }

    @GetMapping("/{messageId}")
    public ApiResponse<MessageDtos.MessageRecord> detail(@PathVariable("messageId") String messageId) {
        return ApiResponse.success(reusableMessageService.getMessage(messageId));
    }

    @GetMapping("/batch/{batchId}")
    public ApiResponse<MessageDtos.BatchDetailResponse> batchDetail(@PathVariable("batchId") String batchId) {
        return ApiResponse.success(reusableMessageService.getBatch(batchId));
    }

    @GetMapping
    public ApiResponse<MessageDtos.ListResponse> list(
        @RequestParam(value = "recipient_type", required = false) String recipientType,
        @RequestParam(value = "recipient", required = false) String recipient,
        @RequestParam(value = "business_code", required = false) String businessCode,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "start_date", required = false) String startDate,
        @RequestParam(value = "end_date", required = false) String endDate,
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "size", defaultValue = "20") Integer size
    ) {
        return ApiResponse.success(
            reusableMessageService.listMessages(recipientType, recipient, businessCode, status, startDate, endDate, page, size)
        );
    }
}
