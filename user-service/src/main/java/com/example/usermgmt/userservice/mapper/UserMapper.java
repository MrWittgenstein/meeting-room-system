package com.example.usermgmt.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.usermgmt.userservice.entity.UserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("SELECT * FROM `user` WHERE legacy_user_id = #{legacyUserId} AND status <> 4 LIMIT 1")
    UserEntity selectByLegacyUserId(@Param("legacyUserId") Integer legacyUserId);

    @Select("SELECT * FROM `user` WHERE legacy_email_hash IN (#{sha256}, #{md5}) AND status <> 4 LIMIT 1")
    UserEntity selectByLegacyEmailHash(@Param("sha256") String sha256, @Param("md5") String md5);

    @Select("""
        SELECT * FROM `user`
        WHERE status <> 4
          AND (
            phone = #{account}
            OR mail = LOWER(TRIM(#{account}))
            OR name = #{account}
            OR legacy_email_hash = SHA2(LOWER(TRIM(#{account})), 256)
            OR legacy_email_hash = MD5(LOWER(TRIM(#{account})))
          )
        LIMIT 1
        """)
    UserEntity selectByAccount(@Param("account") String account);

    @Update("""
        UPDATE `user`
        SET status = #{status},
            phone = NULL,
            mail = NULL,
            last_operator_id = #{operatorId}
        WHERE id = #{userId}
        """)
    int cancelUser(
        @Param("userId") String userId,
        @Param("status") Integer status,
        @Param("operatorId") String operatorId
    );

    @Select("""
        SELECT *
        FROM `user`
        WHERE JSON_UNQUOTE(JSON_EXTRACT(extra_data, '$.feishuOpenId')) = #{openId}
        LIMIT 1
        """)
    UserEntity selectByFeishuOpenId(@Param("openId") String openId);

    @Select("""
        SELECT *
        FROM `user`
        WHERE status <> #{cancelledStatus}
          AND (
            (#{openId} IS NOT NULL AND #{openId} != '' AND JSON_UNQUOTE(JSON_EXTRACT(extra_data, '$.feishuOpenId')) = #{openId})
            OR (#{unionId} IS NOT NULL AND #{unionId} != '' AND JSON_UNQUOTE(JSON_EXTRACT(extra_data, '$.feishuUnionId')) = #{unionId})
            OR (#{userId} IS NOT NULL AND #{userId} != '' AND JSON_UNQUOTE(JSON_EXTRACT(extra_data, '$.feishuUserId')) = #{userId})
          )
        LIMIT 1
        """)
    UserEntity selectByFeishuIdentity(
        @Param("openId") String openId,
        @Param("unionId") String unionId,
        @Param("userId") String userId,
        @Param("cancelledStatus") Integer cancelledStatus
    );

    @Select("""
        <script>
        SELECT COUNT(1)
        FROM `user`
        WHERE 1 = 1
        <if test="keyword != null and keyword != ''">
          AND (id = #{keyword}
            OR name LIKE CONCAT('%', #{keyword}, '%')
            OR phone LIKE CONCAT('%', #{keyword}, '%')
            OR mail LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <choose>
          <when test="status != null">
            AND status = #{status}
          </when>
          <otherwise>
            AND status &lt;&gt; #{excludedStatus}
          </otherwise>
        </choose>
        <if test="role != null and role != ''">
          AND JSON_UNQUOTE(JSON_EXTRACT(extra_data, '$.role')) = #{role}
        </if>
        </script>
        """)
    Long countUsers(
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("role") String role,
        @Param("excludedStatus") Integer excludedStatus
    );

    @Select("""
        <script>
        SELECT *
        FROM `user`
        WHERE 1 = 1
        <if test="keyword != null and keyword != ''">
          AND (id = #{keyword}
            OR name LIKE CONCAT('%', #{keyword}, '%')
            OR phone LIKE CONCAT('%', #{keyword}, '%')
            OR mail LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <choose>
          <when test="status != null">
            AND status = #{status}
          </when>
          <otherwise>
            AND status &lt;&gt; #{excludedStatus}
          </otherwise>
        </choose>
        <if test="role != null and role != ''">
          AND JSON_UNQUOTE(JSON_EXTRACT(extra_data, '$.role')) = #{role}
        </if>
        ORDER BY create_time DESC
        LIMIT #{offset}, #{pageSize}
        </script>
        """)
    List<UserEntity> selectUsers(
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("role") String role,
        @Param("excludedStatus") Integer excludedStatus,
        @Param("offset") long offset,
        @Param("pageSize") int pageSize
    );

    @Select("""
        <script>
        SELECT COUNT(1)
        FROM `user`
        WHERE phone = #{phone}
        <if test="excludeId != null and excludeId != ''">
          AND id &lt;&gt; #{excludeId}
        </if>
        </script>
        """)
    Integer countByPhone(@Param("phone") String phone, @Param("excludeId") String excludeId);

    @Select("""
        <script>
        SELECT COUNT(1)
        FROM `user`
        WHERE mail = #{mail}
        <if test="excludeId != null and excludeId != ''">
          AND id &lt;&gt; #{excludeId}
        </if>
        </script>
        """)
    Integer countByMail(@Param("mail") String mail, @Param("excludeId") String excludeId);
}
