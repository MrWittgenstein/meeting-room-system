package com.uestcfir.service.impl;
import com.github.pagehelper.PageHelper;
import com.uestcfir.exception.ValidationException;
import com.uestcfir.mapper.MeetingRoomStatisticsMapper;
import com.uestcfir.mapper.MeetingroomMapper;
import com.uestcfir.mapper.ReservationMapper;
import com.uestcfir.pojo.dto.MeetingroomDto;
import com.uestcfir.pojo.dto.MeetingroomQueryDto;
import com.uestcfir.pojo.dto.UpdateMeeingroomDto;
import com.uestcfir.pojo.entity.ImageUploadResult;
import com.uestcfir.pojo.entity.Meetingroom;
import com.uestcfir.pojo.entity.Reservations;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.vo.MeetingRoomStatisticsVo;
import com.uestcfir.service.ImageProcessService;
import com.uestcfir.service.MeetroomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import com.uestcfir.pojo.vo.MeetingroomQueryVo;
import com.uestcfir.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

/**
 * 会议室服务实现类
 *
 * @author ylshen
 * @since 1.0.0
 */
@Slf4j
@Service
public class MeetroomImpl implements MeetroomService {
    @Autowired
    MeetingroomMapper meetingroomMapper;
    @Autowired
    ReservationMapper reservationMapper;
    @Autowired
    MeetingRoomStatisticsMapper statisticsMapper;
    @Autowired
    OssFileStorageService ossFileStorageService;
    @Autowired
    ImageProcessService imageProcessService;

    @Override
    public void updateMeetroom(UpdateMeeingroomDto meetingroomDto){
        log.info("Updating meetingroom with id " + meetingroomDto.getRoomId());
        if (!meetingroomDto.getOpenTime().isBefore(meetingroomDto.getCloseTime())) {
            throw new BusinessException("会议室开放时间必须早于关闭时间");
        }
        meetingroomMapper.updateMeetingroomSelective(meetingroomDto);

    }

    @Override
    public MeetingRoomStatisticsVo getStatistics() {
        MeetingRoomStatisticsVo statistics = new MeetingRoomStatisticsVo();

        // 基础统计
        statistics.setTotalRooms(statisticsMapper.getTotalRooms());
        statistics.setReviewedApplications(statisticsMapper.getReviewedApplications());
        statistics.setPendingApplications(statisticsMapper.getPendingApplications());

        // 按类型分类的统计
        statistics.setTodayApprovedByType(statisticsMapper.getTodayApprovedByType());
        statistics.setWeekApprovedByType(statisticsMapper.getWeekApprovedByType());
        statistics.setMonthApprovedByType(statisticsMapper.getMonthApprovedByType());
        statistics.setTodayApplicationsByType(statisticsMapper.getTodayApplicationsByType());
        statistics.setWeekApplicationsByType(statisticsMapper.getWeekApplicationsByType());
        statistics.setMonthApplicationsByType(statisticsMapper.getMonthApplicationsByType());

        return statistics;
    }


    /**
     * 创建会议室
     *
     * @param meetingroomDto 会议室数据传输对象
     * @return 操作结果
     * @throws BusinessException 如果会议室数据无效或创建失败
     */
    public Result createMeetroom(MeetingroomDto meetingroomDto, MultipartFile image) {
        try {
        validateMeetingroomDto(meetingroomDto);
        validateBusinessRules(meetingroomDto);
        log.info("Creating meetingroom successfully " );
        if (image != null){
            imageProcessService.validateImageFile(image);
        }
        Meetingroom meetingroom = new Meetingroom(null,meetingroomDto.getRoomNumber(), meetingroomDto.getRoomName(), meetingroomDto.getCapacity(), meetingroomDto.getLocation(), meetingroomDto.getStatus(), meetingroomDto.getOpenTime(), meetingroomDto.getCloseTime(), meetingroomDto.getType(),null,null ,meetingroomDto.getDescription(),LocalDateTime.now(),LocalDateTime.now());
        Integer affectedRows = meetingroomMapper.insertMeetingroom(meetingroom);
        Integer roomId = meetingroom.getRoomId();
        if(image != null){
            ImageUploadResult imageresult = ossFileStorageService.storeroomimage(image,roomId );
        }
        return Result.success();}
        catch (BusinessException e) {
            log.error("创建会议室失败: " + e.getMessage());
            throw e;
        }
        catch (IOException e) {
            log.error("Creating meetingroom failed: " + e.getMessage());
            throw new BusinessException("创建会议室失败:1111111 " );
        }

    }

    /**
     * 获取可用会议室列表
     *
     * @param meetingroomQueryDto 会议室查询数据传输对象
     * @return 可用会议室列表
     * @throws BusinessException 如果查询参数无效
     */
    public List<MeetingroomQueryVo> getAvailableMeetroomList(MeetingroomQueryDto meetingroomQueryDto) {
        log.info("Getting all meetrooms");
        if (meetingroomQueryDto.hasTimeRange()) {
            meetingroomQueryDto.validateTimeRange();// 时间逻辑检查
                  }
        if (!meetingroomQueryDto.hasTimeRange()){
            if (meetingroomQueryDto.getStartTime() != null || meetingroomQueryDto.getEndTime() != null){
                throw new BusinessException("查询时间范围必须完整:请输入开始时间和结束时间 ");
            }
        }
        Map<String,Object> params = buildParams(meetingroomQueryDto);



        List<MeetingroomQueryVo> meetingroomQueryVo = meetingroomMapper.getAvaiableMeetingrooms(params);

        for (MeetingroomQueryVo vo : meetingroomQueryVo){
            List<Reservations> reservations = reservationMapper.findAllReservationsOfAvailableRoom(vo.getRoomId(), (LocalDate) params.get("queryDate"));
            /**
             * 仅当查询时间范围为空时，才计算可用时间段
             * 即查询为某一天的所有会议室时，可用时间段需要计算
             */
            if (!meetingroomQueryDto.hasTimeRange()){

            vo.setAvailableSlots(buildAvailableSlots(vo.getOpenTime(), vo.getCloseTime(), reservations));
        }

        }

        return meetingroomQueryVo;
    }


    /**
     * 获取所有会议室列表
     *
     * @param page 页码
     * @param size 每页大小
     * @return 会议室列表
     */
    public List<Meetingroom> getAllMeetingroomList(Integer page, Integer size) {
        log.info("Getting all meetrooms");
        try {
            PageHelper.startPage(page, size);
        }
        catch (Exception e) {
            throw new ValidationException("页码和大小不可为空");
        }
        List<Meetingroom> meetingrooms = meetingroomMapper.selectAll();
        return meetingrooms;
    }


    /**
     * 删除会议室
     *
     * @param roomId 会议室ID
     * @throws BusinessException 如果删除失败
     */
    public void deleteMeetroom(Integer roomId) {
        log.info("Deleting meetingroom with id " + roomId);
        reservationMapper.deleteReservationByRoomId(roomId);
        if (meetingroomMapper.deleteMeetingroomById(roomId)) {
            log.info("Deleting meetingroom successfully " + roomId);
        }
        else {
            log.error("Deleting meetingroom failed " + roomId);
            throw new BusinessException("删除会议室失败: " + roomId);
        }

    }








    /**
     * 未检查
     * @param openTime
     * @param closeTime
     * @param reservations
     * @return
     */
    private List<MeetingroomQueryVo.TimeSlot> buildAvailableSlots(LocalTime openTime, LocalTime closeTime, List<Reservations> reservations) {
        List<MeetingroomQueryVo.TimeSlot> availableSlots = new ArrayList<>();

        if (openTime == null || closeTime == null || !openTime.isBefore(closeTime)) {
            return availableSlots;
        }

        // 如果没有预订，整个时间段都可用
        if (reservations == null || reservations.isEmpty()) {
            availableSlots.add(new MeetingroomQueryVo.TimeSlot(openTime, closeTime));
            return availableSlots;
        }

        // 按开始时间排序预订
        //也可以在mapper层排序
        List<Reservations> sortedReservations = reservations.stream()
                .filter(r -> r.getStartTime() != null && r.getEndTime() != null)
                .sorted(Comparator.comparing(Reservations::getStartTime))
                .collect(Collectors.toList());

        LocalTime currentStart = openTime;

        for (Reservations reservation : sortedReservations) {
            LocalTime reservationStart = reservation.getStartTime();
            LocalTime reservationEnd = reservation.getEndTime();

            // 如果预订开始时间在当前时间之后，说明中间有空闲时段
            if (reservationStart.isAfter(currentStart)) {
                // 确保时间段有效（至少有一定时长，比如15分钟）
                if (Duration.between(currentStart, reservationStart).toMinutes() >= 30) {
                    availableSlots.add(new MeetingroomQueryVo.TimeSlot(currentStart, reservationStart));
                }
            }

            // 更新当前时间为预订结束时间（如果更晚的话）
            if (reservationEnd.isAfter(currentStart)) {
                currentStart = reservationEnd;
            }
        }

        // 检查最后一个预订结束后的时间段
        if (currentStart.isBefore(closeTime)) {
            if (Duration.between(currentStart, closeTime).toMinutes() >= 15) {
                availableSlots.add(new MeetingroomQueryVo.TimeSlot(currentStart, closeTime));
            }
        }

        return availableSlots;
  }

    private Map<String,Object> buildParams(MeetingroomQueryDto meetingroomQueryDto) {
        Map<String,Object> params = new HashMap<>();
        if (meetingroomQueryDto.getQueryDate() != null){
            params.put("queryDate", meetingroomQueryDto.getQueryDate());}
        else {
            params.put("queryDate", LocalDate.now());
        }
        if (StringUtils.isNotBlank(meetingroomQueryDto.getLocation())){
            params.put("location", meetingroomQueryDto.getLocation());
        }
        if (meetingroomQueryDto.hasTimeRange()){
            params.put("startTime", meetingroomQueryDto.getStartTime());
            params.put("endTime", meetingroomQueryDto.getEndTime());
        }


        if (meetingroomQueryDto.getMinCapacity() != null){
            params.put("minCapacity", meetingroomQueryDto.getMinCapacity());
        }
        if (meetingroomQueryDto.getMaxCapacity() != null){
            params.put("maxCapacity", meetingroomQueryDto.getMaxCapacity());
            }
        return params;
}













    private void validateMeetingroomDto(MeetingroomDto dto) {
        if (dto == null) {
            throw new BusinessException("会议室数据不能为空");
        }

        if (StringUtils.isBlank(String.valueOf(dto.getRoomNumber()))) {
            throw new BusinessException("房间号不能为空");
        }

        if (StringUtils.isBlank(dto.getRoomName())) {
            throw new BusinessException("房间名称不能为空");
        }

        if (dto.getCapacity() == null || dto.getCapacity() <= 0) {
            throw new BusinessException("容纳人数必须大于0");
        }
    }

    private void validateBusinessRules(MeetingroomDto dto) {
        // 房间号唯一性检查
        Boolean exists = meetingroomMapper.existsByRoomNumber(dto.getRoomNumber());
        if (exists != null && exists) {
            throw new BusinessException("房间号已存在: " + dto.getRoomNumber());
        }

        // 时间逻辑检查
        if (dto.getOpenTime() != null && dto.getCloseTime() != null
                && dto.getOpenTime().isAfter(dto.getCloseTime())) {
            throw new BusinessException("开放时间不能晚于关闭时间");
        }
    }
}
