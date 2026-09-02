package com.uestcfir.controller.approver;


import com.uestcfir.enumeration.user.UserType;
import com.uestcfir.pojo.dto.ApproveDto;
import com.uestcfir.pojo.dto.ReservationDto;
import com.uestcfir.pojo.entity.Reservations;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.vo.ReservationsVo;
import com.uestcfir.service.ReservationService;
import com.uestcfir.utils.JwtUtil;
import com.uestcfir.utils.ReservationConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author ylshen
 * @version 1.0
 * 用于审批预约单的Controller
 */

@RestController
@Slf4j
@RequestMapping("/approver/reservation")
public class ReservationApproveController {


    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationConverter reservationConverter;

    @GetMapping ("getlist")
    public Result getReservationList(@ModelAttribute ReservationDto reservationDto) {
        List<Reservations> reservations=reservationService.getReservationList(reservationDto);
        List<ReservationsVo> voList = reservationConverter.toVoList(reservations);
        return Result.success(voList);

    }

    @PostMapping("approve")
    public Result approveReservation(@RequestBody ApproveDto approveDto, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Map<String, Object> claims = JwtUtil.getClaims(token);
        Integer userId = (Integer) claims.get("UserId");
        Integer userType = (Integer) claims.get("UserType");
        if(userType!= UserType.APPROVER.getCode())
            return Result.fail("无权限操作");

        reservationService.approveReservation(approveDto, userId);
        return Result.success();

    }


}
