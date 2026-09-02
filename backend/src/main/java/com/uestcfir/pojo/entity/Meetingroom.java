package com.uestcfir.pojo.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Meetingroom {
    private Integer roomId;
    private Integer roomNumber;
    private String roomName ;
    private Integer capacity;//房间容量
    private String location;
    private Integer status;//房间状态，0表示空闲，1表示维修中
    private LocalTime openTime;
    private LocalTime closeTime;
    private String type;//房间类型，如多媒体会议室，常规会议室，主席台
    private String originImage;//房间图片url
    private String thumbnailImage;//缩略图url
    private String description;//房间描述
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
