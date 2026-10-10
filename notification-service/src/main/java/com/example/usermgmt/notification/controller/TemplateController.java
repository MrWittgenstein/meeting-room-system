package com.example.usermgmt.notification.controller;

import com.example.usermgmt.common.api.ApiResponse;
import com.example.usermgmt.notification.dto.TemplateDtos;
import com.example.usermgmt.notification.service.ReusableMessageService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final ReusableMessageService reusableMessageService;

    @PostMapping
    public ApiResponse<TemplateDtos.Record> create(@Valid @RequestBody TemplateDtos.CreateRequest request) {
        return ApiResponse.success("Template created", reusableMessageService.createTemplate(request));
    }

    @GetMapping("/{templateId}")
    public ApiResponse<TemplateDtos.Record> detail(@PathVariable("templateId") String templateId) {
        return ApiResponse.success(reusableMessageService.getTemplate(templateId));
    }

    @PutMapping("/{templateId}")
    public ApiResponse<TemplateDtos.Record> update(
        @PathVariable("templateId") String templateId,
        @Valid @RequestBody TemplateDtos.UpdateRequest request
    ) {
        return ApiResponse.success("Template updated", reusableMessageService.updateTemplate(templateId, request));
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> delete(@PathVariable("templateId") String templateId) {
        reusableMessageService.deleteTemplate(templateId);
        return ApiResponse.success("Template archived", null);
    }

    @PostMapping("/{templateId}/publish")
    public ApiResponse<TemplateDtos.Record> publish(
        @PathVariable("templateId") String templateId,
        @RequestBody(required = false) TemplateDtos.PublishRequest request
    ) {
        return ApiResponse.success("Template published", reusableMessageService.publishTemplate(templateId, request));
    }

    @GetMapping
    public ApiResponse<TemplateDtos.ListResponse> list(
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "size", defaultValue = "20") Integer size,
        @RequestParam(value = "business_code", required = false) String businessCode,
        @RequestParam(value = "channel", required = false) String channel,
        @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(reusableMessageService.listTemplates(page, size, businessCode, channel, status));
    }

    @PostMapping("/preview")
    public ApiResponse<TemplateDtos.PreviewResponse> preview(@Valid @RequestBody TemplateDtos.PreviewRequest request) {
        return ApiResponse.success(reusableMessageService.previewTemplate(request));
    }

    @GetMapping("/{templateId}/versions")
    public ApiResponse<List<TemplateDtos.VersionRecord>> versions(@PathVariable("templateId") String templateId) {
        return ApiResponse.success(reusableMessageService.listTemplateVersions(templateId));
    }

    @GetMapping("/{templateId}/versions/{version}")
    public ApiResponse<TemplateDtos.VersionRecord> versionDetail(
        @PathVariable("templateId") String templateId,
        @PathVariable("version") Integer version
    ) {
        return ApiResponse.success(reusableMessageService.getTemplateVersion(templateId, version));
    }

    @PostMapping("/{templateId}/rollback")
    public ApiResponse<TemplateDtos.Record> rollback(
        @PathVariable("templateId") String templateId,
        @Valid @RequestBody TemplateDtos.RollbackRequest request
    ) {
        return ApiResponse.success("Template rolled back", reusableMessageService.rollbackTemplate(templateId, request));
    }
}
