package com.uestcfir.mapper;


import com.uestcfir.pojo.dto.ApproveDto;
import com.uestcfir.pojo.dto.ReservationDto;
import com.uestcfir.pojo.entity.Reservations;


import com.uestcfir.pojo.vo.MeetingroomQueryVo;
import org.apache.ibatis.annotations.*;
import org.springframework.web.bind.annotation.PutMapping;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.time.LocalDate;
import java.util.Map;

@Mapper
public interface ReservationMapper {

    /**
     * 插入预约信息
     * @param reservation
     * @return
     */
    @Insert("insert into reservations(type,room_name,location,user_id,approver_id,room_id,reserve_date,start_time,end_time,status,username,purpose,approve_time,reject_reason,cancel_reason,create_time,update_time) values(#{type},#{roomName},#{location},#{userId},#{approverId},#{roomId},#{reserveDate},#{startTime},#{endTime},#{status},#{username},#{purpose},#{approveTime},#{rejectReason},#{cancelReason},#{createTime},#{updateTime})")
    boolean insertReservation(Reservations reservation);


    /**
     * 根据id查询预约信息
     * @param room_id
     * @param date
     * @return Reservations
     */
    @Select("select * from reservations where room_id = #{room_id} and reserve_date = #{date} and (status = 0 or status = 4) order by start_time for update")
    List<Reservations> findAllReservationsOfAvailableRoom(Integer room_id, LocalDate date);


    //TODO: 由于之后要发邮件提醒，所以可能还有其他status
    @Update("UPDATE reservations SET status = 2, update_time = NOW() " +
            "WHERE status IN (0,3,4) " +
            "AND ((reserve_date < #{date}) OR (reserve_date = #{date} AND end_time < #{now})) " +
            "AND end_time IS NOT NULL")
    int updateExpiredReservations(@Param("now") LocalTime now, @Param("date") LocalDate date);

    //TODO: 删除的status可能需要修改

    //删除过期的预约
    @Delete("delete from reservations where status IN (2,1 )and end_time < #{deadline} and end_time is not null")
    int deleteExpiredReservations(@Param("deadline") LocalDateTime deadline);

    @Delete("delete from reservations where room_id = #{roomId}")
    int deleteReservationByRoomId(Integer roomId);


    //查找未审批的预约
    @Select({
            "<script>",
            "select * from reservations where 1=1",
            "<if test='status!=null'> and status=#{status}</if>",
            "<if test='roomId != null'> and room_id = #{roomId} </if>",
            "<if test='date != null'> and reserve_date = #{date} </if>",
            "<if test='startTime != null'> and start_time &gt;= #{startTime} </if>",
            "<if test='endTime != null'> and end_time &lt;= #{endTime} </if>",


            "</script>"
    })
    List<Reservations> getAllReservations(ReservationDto reservationDto);

    @Update("update reservations set status=#{reservation.approveStatus}, approve_time=#{reservation.approveTime},approver_id=#{approverId},reject_reason=#{reservation.rejectReason} where reservation_id = #{reservation.reservationId}")
    void approveReservation(@Param("reservation") ApproveDto reservation, @Param("approverId") Integer approverId);

    @Select("select * from reservations where user_id = #{userId}")
    List<Reservations> getReservationListByUserId(Integer userId);

    @Delete("delete from reservations where reservation_id = #{reservationId} and user_id = #{userId}")
    boolean cancelReservation(Integer reservationId, Integer userId);

    @Update("UPDATE reservations SET status = #{status}, update_time = NOW() WHERE reservation_id = #{reservationId}")
    void updateReservationStatus(Reservations reservation);

    @Select("SELECT * FROM reservations WHERE reserve_date = CURRENT_DATE AND start_time BETWEEN #{startTime} AND #{endTime} AND (status = 0 or status = 4)")
    List<Reservations> findUpcomingReservations(@Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

    @Select("""
            SELECT COUNT(*) > 0
            FROM reservations
            WHERE user_id = #{userId}
              AND room_id = #{roomId}
              AND reserve_date = #{date}
              AND start_time <= #{time}
              AND end_time >= #{time}
              AND status IN (3, 4)
            """)
    boolean existsActiveReservation(@Param("userId") Integer userId,
                                    @Param("roomId") Integer roomId,
                                    @Param("date") LocalDate date,
                                    @Param("time") LocalTime time);


    /**
     * 查询所有需要更新状态的预约（已通过的预约）
     */
    @Select("SELECT reservation_id, start_time, end_time," +
            "reserve_date, state " +
            "FROM reservations WHERE status = 4")
    List<Reservations> selectApprovedReservations();

    /**
     * 更新预约状态字段
     */
    @Update("UPDATE reservations SET state = #{state}, update_time = NOW() WHERE reservation_id = #{reservationId}")
    void updateReservationState(@Param("reservationId") Integer reservationId, @Param("state") String state);

    /**
     * 查询所有status=2（已过期）的预约
     */
    @Select("SELECT reservation_id, state FROM reservations WHERE status = 2")
    List<Reservations> selectExpiredReservations();
}
