package com.example.usermgmt.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.usermgmt.userservice.entity.PermissionUserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionUserMapper extends BaseMapper<PermissionUserEntity> {
}
