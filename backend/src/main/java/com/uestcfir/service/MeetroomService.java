package com.uestcfir.service;

import com.uestcfir.pojo.dto.MeetingroomDto;
import com.uestcfir.pojo.dto.MeetingroomQueryDto;
import com.uestcfir.pojo.dto.UpdateMeeingroomDto;
import com.uestcfir.pojo.entity.Meetingroom;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.vo.MeetingRoomStatisticsVo;
import com.uestcfir.pojo.vo.MeetingroomQueryVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MeetroomService {
    public Result createMeetroom(MeetingroomDto meetingroomDto, MultipartFile image);

    public List<MeetingroomQueryVo> getAvailableMeetroomList(MeetingroomQueryDto meetingroomQueryDto);

    public List<Meetingroom> getAllMeetingroomList(Integer page, Integer size);


    public void deleteMeetroom(Integer roomId);

    public MeetingRoomStatisticsVo getStatistics();

    void updateMeetroom(UpdateMeeingroomDto meetingroomDto);
}
