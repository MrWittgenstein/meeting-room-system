package com.uestcfir.service;

import com.uestcfir.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

@Service
@Slf4j
public class ImageProcessService {

    /**
     * 生成缩略图
     */
    public byte[] generateThumbnail(MultipartFile originalImage, int width, int height) throws IOException {
        try {
            BufferedImage originalBufferedImage = ImageIO.read(originalImage.getInputStream());

            // 计算缩略图尺寸，保持宽高比
            int originalWidth = originalBufferedImage.getWidth();
            int originalHeight = originalBufferedImage.getHeight();

            int thumbnailWidth, thumbnailHeight;
            if (originalWidth > originalHeight) {
                thumbnailWidth = width;
                thumbnailHeight = (int) (originalHeight * ((double) width / originalWidth));
            } else {
                thumbnailHeight = height;
                thumbnailWidth = (int) (originalWidth * ((double) height / originalHeight));
            }

            // 创建缩略图
            BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumbnail.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalBufferedImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
            g.dispose();

            // 转换为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String formatName = getImageFormat(originalImage.getContentType());
            ImageIO.write(thumbnail, formatName, baos);

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("生成缩略图失败", e);
            throw new IOException("缩略图生成失败", e);
        }
    }

    /**
     * 验证图片文件
     */
    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片文件");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只支持图片格式文件");
        }

        // 验证文件大小 (最大5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("图片大小不能超过5MB");
        }

        // 验证具体格式
        String[] allowedTypes = {"image/jpeg", "image/png", "image/gif", "image/webp"};
        if (!Arrays.asList(allowedTypes).contains(contentType.toLowerCase())) {
            throw new BusinessException("只支持 JPG、PNG、GIF、WebP 格式的图片");
        }
    }

    private String getImageFormat(String contentType) {
        if (contentType == null) return "jpg";

        switch (contentType.toLowerCase()) {
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            default:
                return "jpg";
        }
    }
}