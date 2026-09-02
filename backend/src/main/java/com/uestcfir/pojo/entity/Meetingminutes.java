package com.uestcfir.pojo.entity;


import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Meetingminutes {
    private Integer minuteId;
    private Integer roomId;
    private String title;
    private String content;
    private String participants;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
