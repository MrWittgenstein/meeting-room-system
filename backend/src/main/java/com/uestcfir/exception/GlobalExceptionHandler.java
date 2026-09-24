package com.uestcfir.exception;


import com.uestcfir.pojo.entity.Result;
import com.aliyun.oss.OSSException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler
    public Result handleException(BusinessException e, jakarta.servlet.http.HttpServletResponse response) {
        response.setStatus(e.getStatus());
        log.error("BusinessException occurred: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler
    public Result handleException(IOException e) {
        log.error("IOException occurred: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler
    public Result handleException(Exception e) {
        log.error("Exception occurred: ", e);
        String message = "系统繁忙，请稍后再试";
        if (e instanceof NullPointerException) {
            message = "请求参数缺失或格式错误";
        } else if (e instanceof IllegalArgumentException) {
            message = "请求参数不合法";
        } else if (e instanceof org.springframework.web.bind.MissingServletRequestParameterException) {
            message = "缺少必要请求参数";
        }
        return Result.fail(message);
    }

    @ExceptionHandler
    public Result handleException(ValidationException e) {
        log.error("Exception occurred: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler
    public Result handleException(MaxUploadSizeExceededException e) {
        log.error("File upload size exceeded: {}", e.getMessage());
        return Result.fail("上传文件大小超过限制，最大允许10MB");
    }

    @ExceptionHandler
    public Result handleException(OSSException e) {
        log.error("OSS operation failed: {}", e.getMessage());
        String message = "文件上传失败";
        if (e.getMessage() != null) {
            if (e.getMessage().contains("InvalidAccessKeyId")) {
                message = "文件上传服务配置错误，请联系管理员";
            } else if (e.getMessage().contains("SignatureDoesNotMatch")) {
                message = "文件上传认证失败，请联系管理员";
            } else if (e.getMessage().contains("AccessDenied")) {
                message = "文件上传权限不足，请联系管理员";
            }
        }
        return Result.fail(message);
    }
}
