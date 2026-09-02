package com.uestcfir.service;

import com.uestcfir.pojo.entity.ImageUploadResult;
import com.uestcfir.pojo.entity.Result;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    ImageUploadResult storeImportantNewsAvatar(Integer id, MultipartFile file) throws IOException;

    public ImageUploadResult storeUserAvatar(MultipartFile file, Integer userId) throws IOException;
    public boolean deleteUserAvatar(String filePath);
    public String getFileAccessUrl(String filePath);

    public ImageUploadResult storeroomimage(MultipartFile file, Integer roomId) throws IOException;

}
