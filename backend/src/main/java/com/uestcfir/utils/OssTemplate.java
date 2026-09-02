package com.uestcfir.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.uestcfir.config.OssProperties;
import com.uestcfir.exception.BusinessException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Random;

@Data
@Slf4j
public class OssTemplate {
    private final OSS ossClient;
    private final OssProperties ossProperties;

    public OssTemplate(OSS ossClient, OssProperties ossProperties) {
        this.ossClient = ossClient;
        this.ossProperties = ossProperties;
    }

    /**
     * 上传文件到OSS
     */
    public String uploadFile(MultipartFile file, String filePath) throws IOException {
        try {
            // 创建PutObjectRequest对象
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getBucketName(),
                    filePath,
                    file.getInputStream()
            );

            // 设置对象元信息
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(getContentType(file.getOriginalFilename()));
            metadata.setContentLength(file.getSize());
            putObjectRequest.setMetadata(metadata);

            // 上传文件
            ossClient.putObject(putObjectRequest);

            // 返回完整的访问URL
            return ossProperties.getBaseUrl() + "/" + filePath;

        } catch (OSSException | ClientException e) {
            log.error("OSS上传失败: {}", e.getMessage(), e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 生成预签名URL（用于前端直传）
     */
    public String generatePresignedUrl(String objectName, long expireTime) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireTime * 1000);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    ossProperties.getBucketName(),
                    objectName,
                    HttpMethod.PUT
            );
            request.setExpiration(expiration);
            request.setContentType("image/jpeg"); // 根据实际需要设置

            return ossClient.generatePresignedUrl(request).toString();

        } catch (OSSException | ClientException e) {
            log.error("生成预签名URL失败: {}", e.getMessage(), e);
            throw new BusinessException("生成上传链接失败");
        }
    }

    /**
     * 删除OSS文件
     */
    public void deleteFile(String objectName) {
        try {
            ossClient.deleteObject(ossProperties.getBucketName(), objectName);
        } catch (OSSException | ClientException e) {
            log.error("OSS删除文件失败: {}", e.getMessage(), e);
            throw new BusinessException("文件删除失败");
        }
    }

    /**
     * 检查文件是否存在
     */
    public boolean doesObjectExist(String objectName) {
        try {
            return ossClient.doesObjectExist(ossProperties.getBucketName(), objectName);
        } catch (OSSException | ClientException e) {
            log.error("检查OSS文件存在失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取文件内容类型
     */
    private String getContentType(String filename) {
        String contentType = "application/octet-stream";
        if (filename != null && filename.contains(".")) {
            String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
            switch (extension) {
                case ".jpg":
                case ".jpeg":
                    contentType = "image/jpeg";
                    break;
                case ".png":
                    contentType = "image/png";
                    break;
                case ".gif":
                    contentType = "image/gif";
                    break;
                case ".webp":
                    contentType = "image/webp";
                    break;
                default:
                    contentType = "application/octet-stream";
            }
        }
        return contentType;
    }

    /**
     * 生成头像文件路径
     */
    public String generateAvatarPath(String username, String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.valueOf(new Random().nextInt(1000));
        String extension = getFileExtension(originalFilename);

        return ossProperties.getAvatarDir() + username + "_" + timestamp + "_" + random + extension;
    }

    /**
     * 生成缩略图路径
     */
    public String generateThumbnailPath(String originalPath) {
        int lastDotIndex = originalPath.lastIndexOf(".");
        if (lastDotIndex > 0) {
            return originalPath.substring(0, lastDotIndex) + "_thumb" + originalPath.substring(lastDotIndex);
        }
        return originalPath + "_thumb";
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
