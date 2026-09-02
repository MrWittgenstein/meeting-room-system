package com.uestcfir.controller.approver;



import com.uestcfir.enumeration.user.UserType;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.pojo.dto.MeetingroomDto;
import com.uestcfir.pojo.dto.MeetingroomQueryDto;
import com.uestcfir.pojo.dto.UpdateMeeingroomDto;
import com.uestcfir.pojo.entity.ImageUploadResult;
import com.uestcfir.pojo.entity.Meetingroom;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.vo.MeetingRoomStatisticsVo;
import com.uestcfir.pojo.vo.MeetingroomQueryVo;
import com.uestcfir.service.impl.OssFileStorageService;
import com.uestcfir.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import com.uestcfir.service.MeetroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/approver/meetingroom")
public class MeetingroomController {
    @Autowired
    private MeetroomService meetingroomService;
    @Autowired
    private OssFileStorageService ossFileStorageService;
//统计会议室使用情况
    @GetMapping("/statistics")
    public Result getStatistics() {
        MeetingRoomStatisticsVo statistics = meetingroomService.getStatistics();
        return Result.success(statistics);
    }
    @PostMapping("/create")
    public Result createMeetroom( @RequestPart("meetingroom") MeetingroomDto meetingroomDto, @RequestHeader("Authorization") String authHeader, @RequestPart(value = "image", required = false) MultipartFile image) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType!= 0) {
            throw new BusinessException("只有会议室管理员才能创建会议室");
        }
        return meetingroomService.createMeetroom(meetingroomDto,image);

    }

    @GetMapping("/list")
    public Result getAllMeetroomList(@RequestHeader("Authorization") String authHeader, @RequestParam Integer page, @RequestParam Integer size) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType!= 0) {
            throw new BusinessException("只有会议室管理员才能查看会议室信息");
        }
        if (page == null) {
            page = 1;
        }
        if (size == null) {
            size = 10;
        }
        List<Meetingroom> meetingroomList = meetingroomService.getAllMeetingroomList(page, size);
        return Result.success(meetingroomList);

    }
    @DeleteMapping("/delete/{roomId}")
    public Result deleteMeetroom(@PathVariable Integer roomId, @RequestHeader("Authorization") String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType!= UserType.APPROVER.getCode()) {
            throw new BusinessException("只有会议室管理员才能删除会议室");
        }
        meetingroomService.deleteMeetroom(roomId);
        return Result.success("已删除会议室及其相关预约");
    }
    @PostMapping("/image")
    public Result uploadImage(@RequestPart("file") MultipartFile file, @RequestParam Integer roomId, @RequestHeader("Authorization") String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType!= UserType.APPROVER.getCode()) {
            throw new BusinessException("只有会议室管理员才能上传会议室图片");
        }
        try {
            ImageUploadResult imageresult = ossFileStorageService.storeroomimage(file, roomId);
            return Result.success(imageresult);
        } catch (IOException e) {
            log.error("上传图片失败: {}", e.getMessage(), e);
            return Result.fail("上传图片失败: " + e.getMessage());
        }}
    @PutMapping("/meetroom/change")
    public Result changeMeetroom(@RequestBody UpdateMeeingroomDto meetingroomDto, @RequestHeader("Authorization") String authHeader) {
            Integer[] userinfo = JwtUtil.validateToken(authHeader);
            Integer userId = userinfo[0];
            Integer userType = userinfo[1];
            if (userType!= UserType.APPROVER.getCode()) {
                throw new BusinessException("只有会议室管理员才能修改会议室信息");
            }

            meetingroomService.updateMeetroom(meetingroomDto);
            return Result.success("修改成功");
        }
    }








