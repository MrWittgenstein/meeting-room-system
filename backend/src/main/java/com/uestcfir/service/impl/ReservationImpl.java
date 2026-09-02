package com.uestcfir.service.impl;
import com.github.pagehelper.PageHelper;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.mapper.ReservationMapper;
import com.uestcfir.mapper.UserMapper;
import com.uestcfir.pojo.dto.ApproveDto;
import com.uestcfir.pojo.dto.ReservationDto;
import com.uestcfir.pojo.entity.Reservations;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.entity.User;
import com.uestcfir.service.MeetroomService;
import com.uestcfir.service.ReservationService;
import jakarta.servlet.http.PushBuilder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.time.Duration;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;


/**
 * 预定服务实现类
 *
 * @author ylshen
 * @since 1.0.0
 */
@Service
@Slf4j
public class ReservationImpl implements ReservationService {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private UserMapper userMapper;


    @Autowired
    private ReservationMapper reservationMapper;


    private static final String ROOM_PREFIX = "meetroom:";
    private static final String BOOKING_PREFIX = "reservation:";
    @Autowired
    private MeetroomService meetroomService;


    /**
     *
     * @param reservationDto
     * @param userId
     * @return
     */
    @Override
    /**
     * 添加预定
     *
     * @param reservationDto 预定数据传输对象
     * @param userId 用户ID
     * @return 操作结果
     * @throws BusinessException 如果预定数据无效或添加失败
     */
    public Result addreservation(ReservationDto reservationDto, Integer userId) {
        log.info("Adding reservation: {}", reservationDto);
        String lockKey = ROOM_PREFIX + "lock:" + reservationDto.getRoomId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            if (reservationDto.getDate() == null) {
                reservationDto.setDate(LocalDate.now());
            }


            locked = lock.tryLock(5, 30, TimeUnit.SECONDS); // 等待5秒，锁定30秒
            if (!locked) {
                throw new BusinessException("系统繁忙，请重试");
            }

            validateReservation(reservationDto);

            // 检查时间冲突
            List<Reservations> existingReservations = reservationMapper.findAllReservationsOfAvailableRoom(
                    reservationDto.getRoomId(), reservationDto.getDate());

            for (Reservations existing : existingReservations) {
                if (reservationDto.getStartTime().isBefore(existing.getEndTime()) &&
                        reservationDto.getEndTime().isAfter(existing.getStartTime())) {
                    throw new BusinessException("预约时间与已有预约冲突，请选择其他时间段");
                }
            }
            User user = userMapper.selectByUserId(userId);
            Reservations reservation = new Reservations(
                    reservationDto.getRoomName(),      // roomName
                    reservationDto.getType(),          // type
                    null,                             // reservationId (新建时为null)
                    userId,                           // userId
                    null,                             // approverId
                    reservationDto.getRoomNumber(),    // roomNumber
                    reservationDto.getRoomId(),        // roomId
                    reservationDto.getDate(),          // reserveDate
                    reservationDto.getStartTime(),     // startTime
                    reservationDto.getEndTime(),       // endTime
                    0,                                // status
                    reservationDto.getState(),         // state
                    reservationDto.getPurpose(),       // purpose
                    user.getUsername(),               // username
                    null,                             // approveTime
                    null,                             // rejectReason
                    null,                             // cancelReason
                    LocalDateTime.now(),              // createTime
                    LocalDateTime.now(),              // updateTime
                    reservationDto.getLocation()       // location
            );

            if (reservationMapper.insertReservation(reservation)) {
                return Result.success();
            }
            throw new BusinessException("预定插入失败");

        } catch (BusinessException e) {
            log.error("添加预定失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("添加预定失败: {}", e.getMessage());
            throw new BusinessException("添加预定失败，请检查输入数据");
        } finally {
            // 只有成功获取锁时才释放
            if (locked) {
                try {
                    lock.unlock();
                } catch (IllegalMonitorStateException e) {
                    // 锁可能已经自动释放，忽略此异常
                    log.debug("锁已自动释放: {}", lockKey);
                }
            }
        }
    }

    /**
     *用来返回未审批的预定列表
     * @param reservationDto
     * @return
     */
    @Override
    /**
     * 获取预定列表
     *
     * @param reservationDto 预定查询数据传输对象
     * @return 预定列表
     */
    public List<Reservations> getReservationList(ReservationDto reservationDto){
        // Dto合法性校验
        if (reservationDto.getPage() == null || reservationDto.getPage() < 1) {
            throw new BusinessException("页码必须大于等于1");
        }
        if (reservationDto.getSize() == null || reservationDto.getSize() <= 0) {
            throw new BusinessException("每页大小必须大于0");
        }
        PageHelper.startPage(reservationDto.getPage(), reservationDto.getSize());
        List<Reservations> reservations = reservationMapper.getAllReservations(reservationDto);
        return reservations;

    }

    @Override
    /**
     * 审批预定
     *
     * @param approveDto 审批数据传输对象
     * @param userId 用户ID
     * @return 操作结果
     */
    public Result approveReservation(ApproveDto approveDto, Integer userId) {
        try{
        approveDto.setApproveTime(LocalDateTime.now());
        reservationMapper.approveReservation(approveDto,userId);
        return Result.success();}
        catch (Exception e){
            log.error("审批预定失败: {}", e.getMessage());
            throw new BusinessException("审批预定失败");
        }

    }

    @Override
    /**
     * 根据用户ID获取预定列表
     *
     * @param userId 用户ID
     * @return 预定列表
     */
    public List<Reservations> getReservationListByUserId(Integer userId) {
        // Dto合法性校验


        return reservationMapper.getReservationListByUserId(userId);
    }



    /**
     * 修改预定
     *
    // * @param reservationDto 预定数据传输对象
    // * @param userId 用户ID
    // * @return 操作结果
    // * @throws BusinessException 如果预定数据无效或修改失败
    // */
  ////  public Result changeReservation(ReservationDto reservationDto, Integer userId) {
  //      log.info("Changing reservation: {}", reservationDto);
  //      try {
  //          validateReservation(reservationDto);
  //          Reservations reservation = new Reservations(1, userId, null, reservationDto.getRoomId(), reservationDto.getDate(), reservationDto.getStartTime(), reservationDto.getEndTime(), 0, reservationDto.getPurpose(), LocalDateTime.now(), null, null, LocalDateTime.now(), LocalDateTime.now());
  //          //默认状态为0，表示可用，如果不合理，管理员取消预定，否则默认有效
  //          if (reservationMapper.changeReservation(reservation)) {
  //              return Result.success();
  //          }
  //          throw new BusinessException("预定修改失败");
  //      } catch (BusinessException e) {
  //          log.error("修改预定失败: {}", e.getMessage());
  //          throw e;
  //      } catch (Exception e) {
  //          log.error("修改预定失败: {}", e.getMessage());
  //          throw new BusinessException("修改预定失败，服务器内部错误");
  //      }
  //  }
//
    @Override
    /**
     * 取消预定
     *
     * @param reservationId 预定ID
     * @param userId 用户ID
     * @return 操作结果
     */
    public Result cancelReservation(Integer reservationId, Integer userId) {
        log.info("Cancelling reservation: {}", reservationId);
        try {
            if (reservationMapper.cancelReservation(reservationId,userId)) {
                return Result.success();
            }
            throw new BusinessException("预定取消失败，未找到预约");
        } catch (BusinessException e) {
            log.error("取消预定失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("取消预定失败: {}", e.getMessage());
            throw new BusinessException("取消预定失败，服务器内部错误");
        }
    }
    private void validateReservation(ReservationDto reservationDto) {
        //时间冲突已在返回会议室列表时进行校验
        if (reservationDto == null) {
            throw new BusinessException("预订数据不能为空");
        }
        if (reservationDto.getRoomId() == null) {
            throw new BusinessException("会议室不能为空");
        }
        if (reservationDto.getDate() == null) {
            throw new BusinessException("日期不能为空");
        }
        if (reservationDto.getStartTime() == null) {
            throw new BusinessException("开始时间不能为空");
        }
        if (reservationDto.getEndTime() == null) {
            throw new BusinessException("结束时间不能为空");
        }
        if (reservationDto.getStartTime().isAfter(reservationDto.getEndTime())){
            throw new BusinessException("开始时间不能晚于结束时间");
        }
        if(reservationDto.getDate().isBefore(LocalDate.now())){
            throw new BusinessException("预定日期不能早于当前日期");
        }
        if (reservationDto.getDate().isEqual(LocalDate.now())){
        if (reservationDto.getStartTime().isBefore(LocalTime.now()) || reservationDto.getEndTime().isBefore(LocalTime.now()))
        {
            throw new BusinessException("预定时间不能早于当前时间");
        }}
        if (Duration.between(reservationDto.getStartTime(), reservationDto.getEndTime()).toMinutes() < 60) {
            throw new BusinessException("预约时长不能小于60分钟");
        }
        if (Duration.between(reservationDto.getStartTime(),reservationDto.getEndTime()).toMinutes()>60*6){
            throw new BusinessException("预定时长不能大于6小时");
        }
        if (reservationDto.getStartTime() == null) {
            throw new BusinessException("开始时间不能为空");
        }
        if (reservationDto.getEndTime() == null) {
            throw new BusinessException("结束时间不能为空");
            }
        if (reservationDto.getStartTime().isAfter(reservationDto.getEndTime())){
            throw new BusinessException("开始时间不能晚于结束时间");
        }
        if (Duration.between(reservationDto.getStartTime(), reservationDto.getEndTime()).toMinutes() < 60) {
            throw new BusinessException("预约时长不能小于60分钟");
        }
        if (Duration.between(reservationDto.getEndTime(),reservationDto.getStartTime()).toMinutes()>60*6){
            throw new BusinessException("预定时间不能大于6小时");
        }
    }


}

