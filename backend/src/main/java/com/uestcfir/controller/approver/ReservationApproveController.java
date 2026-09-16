package com.uestcfir.controller.approver;

import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.auth.RolePermissionRegistry;
import com.uestcfir.pojo.dto.ApproveDto;
import com.uestcfir.pojo.dto.ReservationDto;
import com.uestcfir.pojo.entity.Reservations;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.vo.ReservationsVo;
import com.uestcfir.service.ReservationService;
import com.uestcfir.utils.ReservationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/approver/reservation")
public class ReservationApproveController {
    private final ReservationService reservationService;
    private final ReservationConverter reservationConverter;

    @GetMapping("getlist")
    public Result getReservationList(@ModelAttribute ReservationDto reservationDto) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.RESERVATION_APPROVE);
        List<Reservations> reservations = reservationService.getReservationList(reservationDto);
        List<ReservationsVo> voList = reservationConverter.toVoList(reservations);
        return Result.success(voList);
    }

    @PostMapping("approve")
    public Result approveReservation(@RequestBody ApproveDto approveDto) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.RESERVATION_APPROVE);
        reservationService.approveReservation(approveDto, CurrentUserContext.requireUserId());
        return Result.success();
    }
}
