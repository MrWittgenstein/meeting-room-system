package com.uestcfir.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.swing.*;

@AllArgsConstructor
@Data
public class ImageUploadResult {
    private String originalFilePath;     // OSS对象路径
    private String thumbnailFilePath;     // OSS缩略图对象路径
    private String originalFileUrl;     // 原图访问URL
    private String thumbnailFileUrl;     // 缩略图访问URL
    private long fileSize;
    private String contentType;
}
