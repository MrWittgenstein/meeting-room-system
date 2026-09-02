package com.uestcfir.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;
@Data
public class UpdateMeeingroomDto {
    @NotNull(message = "会议室ID不能为空")
    private Integer roomId;
    @NotNull(message = "会议室编号不能为空")
    private Integer roomNumber;
    private String roomName;
    private Integer capacity;
    @NotNull(message = "会议室地址不能为空")
    @NotBlank(message = "会议室地址不能为空")
    private String location;
    private Integer status;//0: available, 1: 维修中
    @NotNull(message = "会议室开放时间不能为空")
    private LocalTime openTime;
    @NotNull(message = "会议室关闭时间不能为空")
    private LocalTime closeTime;

    @NotNull(message = "会议室类型不能为空")
    @NotBlank(message = "会议室类型不能为空")
    @Pattern(regexp = "普通会议室|多媒体会议室|主席台")
    private String type;

    private String description;



}
