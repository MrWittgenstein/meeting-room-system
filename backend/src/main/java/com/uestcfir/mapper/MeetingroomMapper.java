package com.uestcfir.mapper;


import com.uestcfir.pojo.dto.UpdateMeeingroomDto;
import com.uestcfir.pojo.vo.MeetingroomQueryVo;
import org.apache.ibatis.annotations.*;

import com.uestcfir.pojo.entity.Meetingroom;

import java.util.List;
import java.util.Map;

@Mapper
public interface MeetingroomMapper {
    @Insert("insert into meetingroom(room_number, room_name, capacity, location, status,open_time, close_time,type, create_time, update_time) values(#{roomNumber}, #{roomName}, #{capacity}, #{location}, #{status},#{openTime}, #{closeTime},#{type}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "roomId", keyColumn = "room_id")
    Integer insertMeetingroom(Meetingroom meetingroom);

    @Select("select * from meetingroom where room_number = #{room_number}")
    public Boolean existsByRoomNumber(Integer room_number);

    @Select({
            "<script>",
            "SELECT ",
            "mr.room_id as roomId, ",
            "mr.room_number as roomNumber, ",
            "mr.capacity, ",
            "mr.location, ",
            "mr.status, ",
            "mr.open_time as openTime, ",
            "mr.close_time as closeTime, ",
            "mr.type as type,",
            "mr.room_name as roomName,",
            "mr.origin_image as originImage,",
            "CASE ",
            "  WHEN #{startTime} IS NOT NULL AND #{endTime} IS NOT NULL THEN ",
            "    NOT EXISTS (",
            "      SELECT 1 FROM reservations mb ",
            "      WHERE mb.room_id = mr.room_id ",
            "      AND mb.reserve_date = #{queryDate} ",
            "      AND (mb.status = 0 OR  mb.status=4) ",
            "      AND mb.start_time &lt; #{endTime} ",
            "      AND mb.end_time &gt; #{startTime} ",
            "    ) ",
            "  ELSE true ",
            "END as isAvailable ",
            "FROM meetingroom mr ",
            "WHERE mr.status = 0 ",
            "<if test='minCapacity != null'> AND mr.capacity &gt;= #{minCapacity} </if>",
            "<if test='maxCapacity != null'> AND mr.capacity &lt;= #{maxCapacity} </if>",
            "<if test='location != null and location != \"\"'> AND mr.location = #{location} </if>",
            "<if test='startTime != null and endTime != null'> ",
            "  AND mr.open_time &lt;= #{startTime} ",
            "  AND mr.close_time &gt;= #{endTime} ",
            "</if>",
            "ORDER BY mr.room_number",
            "</script>"
    })
    public List<MeetingroomQueryVo> getAvaiableMeetingrooms(Map<String, Object> params);

    @Select("select * from meetingroom where room_id = #{roomId}")
    public Meetingroom getMeetingroomById(Integer roomId);

    @Select("select * from meetingroom where room_number = #{roomNumber}")
    public Meetingroom getMeetingroomByRoomNumber(Integer roomNumber);


    @Select("select room_id, room_number, room_name, capacity, location, status, open_time, close_time, create_time, update_time,type, origin_image, thumbnail_image, description from meetingroom ")
    public List<Meetingroom> selectAll();

    @Delete("delete from meetingroom where room_id = #{roomId}")
    public boolean deleteMeetingroomById(Integer roomId);

    @Update("update meetingroom set origin_image = #{originalFileUrl}, thumbnail_image = #{thumbnailFileUrl} where room_id = #{roomId}")
    boolean updateImageUrl( Integer roomId, String originalFileUrl, String thumbnailFileUrl);

    @Update({
            "<script>",
            "UPDATE meetingroom",
            "<set>",
            "  <if test='roomNumber != null'>room_number = #{roomNumber},</if>",
            "  <if test='roomName != null and roomName != \"\"'>room_name = #{roomName},</if>",
            "  <if test='capacity != null'>capacity = #{capacity},</if>",
            "  <if test='location != null and location != \"\"'>location = #{location},</if>",
            "  <if test='status != null'>status = #{status},</if>",
            "  <if test='openTime != null'>open_time = #{openTime},</if>",
            "  <if test='closeTime != null'>close_time = #{closeTime},</if>",
            "  <if test='type != null and type != \"\"'>type = #{type},</if>",
            "  <if test='description != null'>description = #{description},</if>",
            "  update_time = NOW()",
            "</set>",
            "WHERE room_id = #{roomId}",
            "</script>"
    })
    int updateMeetingroomSelective(UpdateMeeingroomDto meetingroomDto);

}
