package com.example.usermgmt.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.usermgmt.userservice.entity.AttributeDataEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AttributeDataMapper extends BaseMapper<AttributeDataEntity> {

    @Select("""
        SELECT *
        FROM attribute_data
        WHERE group_id = #{groupId}
          AND user_id IS NULL
        ORDER BY create_time ASC
        """)
    List<AttributeDataEntity> selectGroupAttributes(@Param("groupId") String groupId);

    @Select("""
        SELECT *
        FROM attribute_data
        WHERE user_id = #{userId}
          AND group_id IS NULL
        ORDER BY create_time ASC
        """)
    List<AttributeDataEntity> selectUserAttributes(@Param("userId") String userId);
}
