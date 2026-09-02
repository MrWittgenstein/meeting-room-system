package com.uestcfir.controller.user;

import com.uestcfir.enumeration.reservation.ReservationStatus;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.pojo.dto.MeetingroomQueryDto;
import com.uestcfir.pojo.dto.ReservationDto;
import com.uestcfir.pojo.entity.Reservations;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.vo.MeetingroomQueryVo;
import com.uestcfir.pojo.vo.ReservationsVo;
import com.uestcfir.service.MeetroomService;
import com.uestcfir.service.ReservationService;

import com.uestcfir.utils.ReservationConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.uestcfir.utils.JwtUtil;

import java.util.List;
import java.util.Map;

/**
 * 用户预定控制器
 *
 * @author ylshen
 * @since 1.0.0
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class ReservationController {
    @Autowired
    private ReservationService reservationService;

    @Autowired
    private MeetroomService meetingroomService;

    @Autowired
    private ReservationConverter reservationConverter;
    @PostMapping("/addreservation")
    /**
     * 添加预定
     *
     * @param reservations 预定数据传输对象
     * @param authHeader 认证头信息
     * @return 操作结果
     * @throws BusinessException 如果预定数据无效或添加失败
     */
    public Result addreservation(@RequestBody ReservationDto reservations, @RequestHeader("Authorization")String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];

        //判断用户是否为管理员
        if (userType!= 1) {
            return Result.fail("管理员不可预定");
        }

        log.info("addreservation user: {}", reservations);
        return reservationService.addreservation(reservations,userId);
    }


    /**
     会议室列表查询接口
     先查询会议室列表，然后预约
     */
    @PostMapping("/getmeetingrooms")
    /**
     * 获取会议室列表
     *
     * @param meetingroomQueryDto 会议室查询数据传输对象
     * @return 会议室列表
     * @throws BusinessException 如果查询参数无效
     */
    public Result getmeetingrooms(@ModelAttribute MeetingroomQueryDto meetingroomQueryDto) throws BusinessException {
        log.info("查询会议室列表，参数: {}", meetingroomQueryDto);
        List<MeetingroomQueryVo> queryList = meetingroomService.getAvailableMeetroomList(meetingroomQueryDto);
        return Result.success(queryList);
    }

    @GetMapping ("/getreservations")
    /**
     * 获取预定列表
     *
     * @param authHeader 认证头信息
     * @return 预定列表
     * @throws BusinessException 如果认证失败或参数无效
     */
    public Result getreservations(@RequestHeader("Authorization") String authHeader) throws BusinessException {
        if (!authHeader.startsWith("Bearer ")) {
            throw new BusinessException("无效的认证头信息");
        }
        String token = authHeader.substring(7);
        Map<String, Object> claims;
        try {
            claims = JwtUtil.getClaims(token);
        } catch (Exception e) {
            throw new BusinessException("无效的 Token");
        }
        Integer userId = (Integer) claims.get("UserId");
        Integer userType = (Integer) claims.get("UserType");
        log.info("获取用户 {} 的预定列表，用户类型: {}", userId, userType);
        List<Reservations> reservations = reservationService.getReservationListByUserId(userId);
        List<ReservationsVo> voList = reservationConverter.toVoList(reservations);
        log.info(voList.toString());
        return Result.success(voList);
    }



    @DeleteMapping("/cancelreservation")
    /**
     * 取消预定
     * @param reservationId 预定ID
     * @param authHeader 认证头信息
     * @return 操作结果
     * @throws BusinessException 如果取消失败
     */
    public Result cancelreservation(@RequestParam("reservationId") Integer reservationId, @RequestHeader("Authorization")String authHeader) {
        String token = authHeader.substring(7);//去掉前缀Bearer,从第七位开始截取token
        Map<String, Object> claims = JwtUtil.getClaims(token);
        Integer userId = (Integer) claims.get("UserId");
        Integer userType = (Integer) claims.get("UserType");
        //判断用户是否为管理员
        if (userType!= 1) {
            return Result.fail("UserType error");
        }
        log.info("cancelreservation reservationId: {}", reservationId);
        return reservationService.cancelReservation(reservationId,userId);

        }

}
