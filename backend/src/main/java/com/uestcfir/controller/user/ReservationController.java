package com.uestcfir.controller.user;

import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.auth.RolePermissionRegistry;
import com.uestcfir.pojo.dto.MeetingroomQueryDto;
import com.uestcfir.pojo.dto.ReservationDto;
import com.uestcfir.pojo.entity.Reservations;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.vo.MeetingroomQueryVo;
import com.uestcfir.pojo.vo.ReservationsVo;
import com.uestcfir.service.MeetroomService;
import com.uestcfir.service.ReservationService;
import com.uestcfir.utils.ReservationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class ReservationController {
    private final ReservationService reservationService;
    private final MeetroomService meetingroomService;
    private final ReservationConverter reservationConverter;

    @PostMapping("/addreservation")
    public Result addReservation(@RequestBody ReservationDto reservations) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.RESERVATION_CREATE);
        return reservationService.addreservation(reservations, CurrentUserContext.requireUserId());
    }

    @PostMapping("/getmeetingrooms")
    public Result getMeetingRooms(@ModelAttribute MeetingroomQueryDto meetingroomQueryDto) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.ROOM_READ);
        List<MeetingroomQueryVo> queryList = meetingroomService.getAvailableMeetroomList(meetingroomQueryDto);
        return Result.success(queryList);
    }

    @GetMapping("/getreservations")
    public Result getReservations() {
        CurrentUserContext.requirePermission(RolePermissionRegistry.RESERVATION_READ_SELF);
        List<Reservations> reservations = reservationService.getReservationListByUserId(CurrentUserContext.requireUserId());
        return Result.success(reservationConverter.toVoList(reservations));
    }

    @DeleteMapping("/cancelreservation")
    public Result cancelReservation(@RequestParam("reservationId") Integer reservationId) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.RESERVATION_CANCEL_SELF);
        return reservationService.cancelReservation(reservationId, CurrentUserContext.requireUserId());
    }
}
