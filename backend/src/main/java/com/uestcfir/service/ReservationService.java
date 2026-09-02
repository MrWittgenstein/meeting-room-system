package com.uestcfir.service;

import com.uestcfir.pojo.dto.ApproveDto;
import com.uestcfir.pojo.dto.ReservationDto;
import com.uestcfir.pojo.entity.Reservations;
import com.uestcfir.pojo.entity.Result;
import java.util.List;

public interface ReservationService {


    public Result addreservation(ReservationDto reservationDto, Integer userId);

    List<Reservations> getReservationList(ReservationDto reservationDto);

    Result approveReservation(ApproveDto reservationDto, Integer userId);

    List<Reservations> getReservationListByUserId(Integer userId);

    Result cancelReservation(Integer reservationId,Integer userId);
}
