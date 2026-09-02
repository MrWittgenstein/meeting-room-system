package com.uestcfir.service.impl;

import com.uestcfir.mapper.ImportantNewsMapper;
import com.uestcfir.mapper.MeetingroomMapper;
import com.uestcfir.mapper.UserMapper;
import com.uestcfir.pojo.entity.ImageUploadResult;
import com.uestcfir.service.ImportantNewsService;
import com.uestcfir.service.FileStorageService;
import com.uestcfir.service.ImageProcessService;
import com.uestcfir.utils.OssTemplate;
import com.uestcfir.utils.ByteArrayMultipartFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Slf4j
public class OssFileStorageService implements FileStorageService {

    @Autowired
    private OssTemplate ossTemplate;

    @Autowired
    private ImageProcessService imageProcessService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MeetingroomMapper meetingroomMapper;

    @Autowired
    private ImportantNewsService importantNewsService;

    @Autowired
    private ImportantNewsMapper importantNewsMapper;


    @Override
    public ImageUploadResult storeImportantNewsAvatar(Integer id, MultipartFile file) throws IOException{
        // 验证文件
        imageProcessService.validateImageFile(file);

        // 生成文件路径
        String originalFilePath = ossTemplate.generateAvatarPath(id.toString(), file.getOriginalFilename());
        String thumbnailFilePath = ossTemplate.generateThumbnailPath(originalFilePath);

        // 上传原图到OSS
        String originalFileUrl = ossTemplate.uploadFile(file, originalFilePath);
        log.info("原图上传OSS成功: importantnewsid={}, URL={}", id, originalFileUrl);
        // 生成并上传缩略图
        String thumbnailFileUrl = uploadThumbnail(file, thumbnailFilePath);

        if (!importantNewsMapper.uploadImportantNewsUrl(id,originalFileUrl)){

            log.error("重要事件图片URL上传失败；重要事件id={},原图={}",id,originalFileUrl);
            throw new IOException("重要事件图片信息更新失败");

        }

        return new ImageUploadResult(
                originalFilePath,      // OSS对象路径
                thumbnailFilePath,     // OSS缩略图对象路径
                originalFileUrl,       // 原图访问URL
                thumbnailFileUrl,      // 缩略图访问URL
                file.getSize(),
                file.getContentType()
        );



    }
    @Override
    public ImageUploadResult storeUserAvatar(MultipartFile file, Integer userId) throws IOException {
        // 验证文件
        imageProcessService.validateImageFile(file);

        // 生成文件路径
        String originalFilePath = ossTemplate.generateAvatarPath(userId.toString(), file.getOriginalFilename());
        String thumbnailFilePath = ossTemplate.generateThumbnailPath(originalFilePath);

        // 上传原图到OSS
        String originalFileUrl = ossTemplate.uploadFile(file, originalFilePath);

        // 生成并上传缩略图
        String thumbnailFileUrl = uploadThumbnail(file, thumbnailFilePath);

        if (!userMapper.updateAvatarUrl(userId, originalFileUrl,thumbnailFileUrl)){
            log.error("头像上传OSS失败: 用户={}, 原图={}, 缩略图={}", userId, originalFileUrl, thumbnailFileUrl);
            throw new IOException("头像上传OSS失败");

        };
        log.info("头像上传OSS成功: 用户={}, 原图={}, 缩略图={}", userId, originalFileUrl, thumbnailFileUrl);

        return new ImageUploadResult(
                originalFilePath,      // OSS对象路径
                thumbnailFilePath,     // OSS缩略图对象路径
                originalFileUrl,       // 原图访问URL
                thumbnailFileUrl,      // 缩略图访问URL
                file.getSize(),
                file.getContentType()
        );
    }

    /**
     * 生成并上传缩略图
     */
    @Override
    public ImageUploadResult storeroomimage(MultipartFile file, Integer roomId) throws IOException {
        try {
            // 验证文件
            imageProcessService.validateImageFile(file);



            // 生成文件路径
            String originalFilePath = ossTemplate.generateAvatarPath(roomId.toString(), file.getOriginalFilename());
            String thumbnailFilePath = ossTemplate.generateThumbnailPath(originalFilePath);



            // 上传原图到OSS
            String originalFileUrl = ossTemplate.uploadFile(file, originalFilePath);
            log.info("原图上传OSS成功: roomId={}, URL={}", roomId, originalFileUrl);

            // 生成并上传缩略图
            String thumbnailFileUrl = uploadThumbnail(file, thumbnailFilePath);
            log.info("缩略图上传OSS成功: roomId={}, URL={}", roomId, thumbnailFileUrl);

            // 更新数据库
            boolean updateSuccess = meetingroomMapper.updateImageUrl(roomId, originalFileUrl, thumbnailFileUrl);

            if (!updateSuccess) {
                log.error("会议室图片URL更新失败: roomId={}, 原图={}, 缩略图={}", roomId, originalFileUrl, thumbnailFileUrl);
                // 可选：删除已上传的OSS文件
                // deleteUploadedFiles(originalFilePath, thumbnailFilePath);
                throw new IOException("会议室图片信息更新失败");
            }

            log.info("会议室图片上传完成: roomId={}, 原图={}, 缩略图={}", roomId, originalFileUrl, thumbnailFileUrl);

            return new ImageUploadResult(
                    originalFilePath,      // OSS对象路径
                    thumbnailFilePath,     // OSS缩略图对象路径
                    originalFileUrl,       // 原图访问URL
                    thumbnailFileUrl,      // 缩略图访问URL
                    file.getSize(),
                    file.getContentType()
            );

        } catch (Exception e) {
            log.error("会议室图片上传过程失败: roomId={}, 错误信息={}", roomId, e.getMessage(), e);
            if (e instanceof IOException) {
                throw e;
            } else {
                throw new IOException("会议室图片上传失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 上传缩略图
     */


    private String uploadThumbnail(MultipartFile originalImage, String thumbnailPath) throws IOException {
        try {
            // 生成缩略图字节数组
            byte[] thumbnailBytes = imageProcessService.generateThumbnail(originalImage, 100, 100);

            // 创建缩略图的MultipartFile
            MultipartFile thumbnailFile = new ByteArrayMultipartFile(
                    thumbnailBytes,
                    "thumbnail",
                    thumbnailPath.substring(thumbnailPath.lastIndexOf("/") + 1),
                    "image/jpeg",
                    thumbnailBytes.length
            );

            // 上传缩略图到OSS
            return ossTemplate.uploadFile(thumbnailFile, thumbnailPath);

        } catch (Exception e) {
            log.warn("缩略图生成失败，将继续使用原图", e);
            // 如果缩略图生成失败，返回原图URL
            return null;
        }
    }

    @Override
    public boolean deleteUserAvatar(String filePath) {
        try {
            ossTemplate.deleteFile(filePath);

            // 同时删除缩略图
            String thumbnailPath = ossTemplate.generateThumbnailPath(filePath);
            if (ossTemplate.doesObjectExist(thumbnailPath)) {
                ossTemplate.deleteFile(thumbnailPath);
            }

            return true;
        } catch (Exception e) {
            log.error("删除OSS头像文件失败: {}", filePath, e);
            return false;
        }
    }

    @Override
    public String getFileAccessUrl(String filePath) {
        return ossTemplate.generatePresignedUrl(filePath, 3600);
    }


}

