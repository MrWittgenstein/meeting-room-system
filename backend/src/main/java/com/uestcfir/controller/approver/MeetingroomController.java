package com.uestcfir.controller.approver;

import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.auth.RolePermissionRegistry;
import com.uestcfir.pojo.dto.MeetingroomDto;
import com.uestcfir.pojo.dto.UpdateMeeingroomDto;
import com.uestcfir.pojo.entity.ImageUploadResult;
import com.uestcfir.pojo.entity.Meetingroom;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.vo.MeetingRoomStatisticsVo;
import com.uestcfir.service.MeetroomService;
import com.uestcfir.service.impl.OssFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/approver/meetingroom")
public class MeetingroomController {
    private final MeetroomService meetingroomService;
    private final OssFileStorageService ossFileStorageService;

    @GetMapping("/statistics")
    public Result getStatistics() {
        CurrentUserContext.requirePermission(RolePermissionRegistry.STATISTICS_READ);
        MeetingRoomStatisticsVo statistics = meetingroomService.getStatistics();
        return Result.success(statistics);
    }

    @PostMapping("/create")
    public Result createMeetroom(@RequestPart("meetingroom") MeetingroomDto meetingroomDto,
                                 @RequestPart(value = "image", required = false) MultipartFile image) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.ROOM_MANAGE);
        return meetingroomService.createMeetroom(meetingroomDto, image);
    }

    @GetMapping("/list")
    public Result getAllMeetroomList(@RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer size) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.ROOM_MANAGE);
        List<Meetingroom> meetingroomList = meetingroomService.getAllMeetingroomList(page, size);
        return Result.success(meetingroomList);
    }

    @DeleteMapping("/delete/{roomId}")
    public Result deleteMeetroom(@PathVariable Integer roomId) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.ROOM_MANAGE);
        meetingroomService.deleteMeetroom(roomId);
        return Result.success("已删除会议室及其相关预约");
    }

    @PostMapping("/image")
    public Result uploadImage(@RequestPart("file") MultipartFile file, @RequestParam Integer roomId) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.ROOM_MANAGE);
        try {
            ImageUploadResult imageResult = ossFileStorageService.storeroomimage(file, roomId);
            return Result.success(imageResult);
        } catch (IOException e) {
            log.error("upload meeting room image failed", e);
            return Result.fail("上传图片失败");
        }
    }

    @PutMapping("/meetroom/change")
    public Result changeMeetroom(@RequestBody UpdateMeeingroomDto meetingroomDto) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.ROOM_MANAGE);
        meetingroomService.updateMeetroom(meetingroomDto);
        return Result.success("修改成功");
    }
}
