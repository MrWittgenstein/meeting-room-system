package com.uestcfir.controller.admin;

import com.uestcfir.enumeration.user.UserType;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.pojo.entity.ImageUploadResult;
import com.uestcfir.pojo.entity.ImportantNews;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.service.ImportantNewsService;
import com.uestcfir.service.impl.OssFileStorageService;
import com.uestcfir.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 重要事件管理控制器
 */
// 生成并上传缩略图

@Slf4j
@RestController
@RequestMapping("/importantnews")
public class ImportantNewsController {

    @Autowired
    private ImportantNewsService importantNewsService;

    @Autowired
    private OssFileStorageService ossFileStorageService;

    @PostMapping("/add")
    public boolean addImportantNews(@RequestBody ImportantNews importantNews, @RequestHeader("Authorization") String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType != 0) {
            throw new BusinessException("只有管理员才能添加重要事件");
       }

        // 设置发布人信息
        importantNews.setPublisherId(userId);
        // 设置创建时间为当前时间
        importantNews.setCreateTime(java.time.LocalDateTime.now());

        return importantNewsService.addImportantNews(importantNews);
    }

    @GetMapping("/all")
    public List<ImportantNews> getAllImportantNews(@RequestHeader("Authorization") String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
       // if (userType != 0) {
     //       throw new BusinessException("只有管理员才能查看重要事件");
    //    }

        return importantNewsService.getAllImportantNews();
    }

    @GetMapping("/status/valid")
    public List<ImportantNews> getUnpublishedImportantNews(@RequestHeader("Authorization") String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType != 0) {
            throw new BusinessException("只有管理员才能查看重要事件");
        }
        return importantNewsService.getImportantNewsByStatus("未过期");
    }

    @PutMapping("/update")
    public boolean updateImportantNews(@RequestBody ImportantNews importantNews, @RequestHeader("Authorization") String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType != 0) {
            throw new BusinessException("只有管理员才能修改重要事件");
        }
        return importantNewsService.updateImportantNews(importantNews);
    }

    @DeleteMapping("/{id}")
    public boolean deleteImportantNews(@PathVariable Integer id, @RequestHeader("Authorization") String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType != 0) {
            throw new BusinessException("只有管理员才能删除重要事件");
        }
        return importantNewsService.deleteImportantNews(id);
    }

    @PostMapping("/uploadPicture")
    public Result uploadPicture(@RequestPart("file") MultipartFile file,@RequestParam Integer id,@RequestHeader("Authorization") String authHeader){
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType!= UserType.APPROVER.getCode()) {
            throw new BusinessException("只有会议室管理员才能上传重要事件图片");
        }
        try {
            ImageUploadResult imageresult = ossFileStorageService.storeImportantNewsAvatar(id,file);
            return Result.success(imageresult);
        } catch (IOException e) {
            log.error("上传图片失败: {}", e.getMessage(), e);
            return Result.fail("上传图片失败: " + e.getMessage());
        }

    }

    /**
     * 按优先级获取重要事件（最多8条）
     * 按紧急→高→中→低的顺序补充
     */
    @GetMapping("/priority")
    public List<ImportantNews> getImportantNewsByPriority(@RequestHeader("Authorization") String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];


      //  if (userType!= UserType.APPROVER.getCode()) {
     //       throw new BusinessException("只有会议室管理员才能上传重要事件图片");
     //   }



        // 调用service方法，limit固定为8
        return importantNewsService.getImportantNewsByPriority(8);
    }



}
