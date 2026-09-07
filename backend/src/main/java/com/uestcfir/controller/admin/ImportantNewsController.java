package com.uestcfir.controller.admin;

import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.auth.RolePermissionRegistry;
import com.uestcfir.pojo.entity.ImageUploadResult;
import com.uestcfir.pojo.entity.ImportantNews;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.service.ImportantNewsService;
import com.uestcfir.service.impl.OssFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/importantnews")
public class ImportantNewsController {
    private final ImportantNewsService importantNewsService;
    private final OssFileStorageService ossFileStorageService;

    @PostMapping("/add")
    public boolean addImportantNews(@RequestBody ImportantNews importantNews) {
        requireManage();
        importantNews.setPublisherId(CurrentUserContext.requireUserId());
        importantNews.setCreateTime(java.time.LocalDateTime.now());
        return importantNewsService.addImportantNews(importantNews);
    }

    @GetMapping("/all")
    public List<ImportantNews> getAllImportantNews() {
        CurrentUserContext.requirePermission(RolePermissionRegistry.NEWS_READ);
        return importantNewsService.getAllImportantNews();
    }

    @GetMapping("/status/valid")
    public List<ImportantNews> getUnpublishedImportantNews() {
        requireManage();
        return importantNewsService.getImportantNewsByStatus("未过期");
    }

    @PutMapping("/update")
    public boolean updateImportantNews(@RequestBody ImportantNews importantNews) {
        requireManage();
        return importantNewsService.updateImportantNews(importantNews);
    }

    @DeleteMapping("/{id}")
    public boolean deleteImportantNews(@PathVariable Integer id) {
        requireManage();
        return importantNewsService.deleteImportantNews(id);
    }

    @PostMapping("/uploadPicture")
    public Result uploadPicture(@RequestPart("file") MultipartFile file, @RequestParam Integer id) {
        requireManage();
        try {
            ImageUploadResult imageResult = ossFileStorageService.storeImportantNewsAvatar(id, file);
            return Result.success(imageResult);
        } catch (IOException e) {
            log.error("upload important news image failed", e);
            return Result.fail("上传图片失败");
        }
    }

    @GetMapping("/priority")
    public List<ImportantNews> getImportantNewsByPriority() {
        CurrentUserContext.requirePermission(RolePermissionRegistry.NEWS_READ);
        return importantNewsService.getImportantNewsByPriority(8);
    }

    private void requireManage() {
        CurrentUserContext.requirePermission(RolePermissionRegistry.NEWS_MANAGE);
    }
}
