package com.example.usermgmt.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Map;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LoginUserMapper extends BaseMapper<Map<String, Object>> {

    @Delete("DELETE FROM login_user WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") String userId);

    @Insert("""
        INSERT INTO login_user (id, credential_type, user_id, login_ip, login_result)
        VALUES (#{id}, #{credentialType}, #{userId}, #{loginIp}, #{loginResult})
        """)
    int insertLoginRecord(
        @Param("id") String id,
        @Param("credentialType") String credentialType,
        @Param("userId") String userId,
        @Param("loginIp") String loginIp,
        @Param("loginResult") Integer loginResult
    );

    @Select("""
        SELECT login_time AS loginTime, login_ip AS loginIp
        FROM login_user
        WHERE user_id = #{userId} AND login_result = 1
        ORDER BY login_time DESC
        LIMIT 1
        """)
    Map<String, Object> selectLatestSuccessByUserId(@Param("userId") String userId);
}
