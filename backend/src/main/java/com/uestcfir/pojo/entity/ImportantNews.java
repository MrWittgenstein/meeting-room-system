package com.uestcfir.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 重要事件实体类
 * 对应数据库表 importantnews
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportantNews {

    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 重要信息标题
     */
    private String title;

    /**
     * 详细内容描述
     */
    private String content;

    /**
     * 关联的会议室ID
     */
    private Integer relatedRoomId;

    /**
     * 信息类型：维修、删除、暂停使用、恢复使用、其他重要通知
     */
    private String newsType;

    /**
     * 生效开始时间
     */
    private LocalDateTime startTime;

    /**
     * 生效结束时间
     */
    private LocalDateTime endTime;

    /**
     * 严重程度：低、中、高、紧急
     */
    private String severity;

    /**
     * 信息状态：未发布、已发布、已过期、已撤销
     */
    private String status;

    /**
     * 发布人ID
     */
    private Integer publisherId;

    /**
     * 发布人姓名
     */
    private String publisherName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
