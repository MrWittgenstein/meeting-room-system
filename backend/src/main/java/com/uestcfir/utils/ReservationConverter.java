package com.uestcfir.utils;

import com.uestcfir.pojo.entity.Reservations;
import com.uestcfir.pojo.vo.ReservationsVo;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReservationConverter {

    public ReservationsVo toVo(Reservations reservation) {
        if (reservation == null) {
            return null;
        }

        ReservationsVo vo = new ReservationsVo();
        vo.setReservationId(reservation.getReservationId());
        vo.setRoomId(reservation.getRoomId());
        vo.setReserveDate(reservation.getReserveDate());
        vo.setStartTime(reservation.getStartTime());
        vo.setEndTime(reservation.getEndTime());
        vo.setPurpose(reservation.getPurpose());
        vo.setApproveTime(reservation.getApproveTime());
        vo.setRejectReason(reservation.getRejectReason());
        vo.setStatus(convertStatus(reservation.getStatus()));
        // 缺失的字段设置：
        vo.setRoomName(reservation.getRoomName());      // ❌ 缺失
        vo.setLocation(reservation.getLocation());      // ❌ 缺失
        vo.setRoomNumber(reservation.getRoomNumber());  // ❌ 缺失
        vo.setState(reservation.getState());            // ❌ 缺失
        vo.setUsername(reservation.getUsername());
        vo.setType(reservation.getType());
        return vo;
    }

    public List<ReservationsVo> toVoList(List<Reservations> reservations) {
        if (reservations == null) {
            return Collections.emptyList();
        }

        return reservations.stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    private String convertStatus(Integer statusCode) {
        if (statusCode == null) return "未知状态";
        switch (statusCode) {
            case 0: return "待确认";
            case 1: return "已拒绝";
            case 2: return "已过期";//4.已通过 5.已拒绝
            case 3: return "使用中";
            case 4: return "已通过";
            default: return "未知状态";
        }
    }
}