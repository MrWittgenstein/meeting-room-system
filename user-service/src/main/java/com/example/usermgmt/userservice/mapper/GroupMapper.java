package com.example.usermgmt.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.usermgmt.userservice.entity.GroupEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GroupMapper extends BaseMapper<GroupEntity> {

    @Select("""
        <script>
        SELECT COUNT(1)
        FROM `group`
        WHERE 1 = 1
        <if test="keyword != null and keyword != ''">
          AND (name LIKE CONCAT('%', #{keyword}, '%')
            OR description LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null">
          AND status = #{status}
        </if>
        </script>
        """)
    Long countGroups(@Param("keyword") String keyword, @Param("status") Integer status);

    @Select("""
        <script>
        SELECT *
        FROM `group`
        WHERE 1 = 1
        <if test="keyword != null and keyword != ''">
          AND (name LIKE CONCAT('%', #{keyword}, '%')
            OR description LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null">
          AND status = #{status}
        </if>
        ORDER BY create_time DESC
        LIMIT #{offset}, #{pageSize}
        </script>
        """)
    List<GroupEntity> selectGroups(
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("offset") long offset,
        @Param("pageSize") int pageSize
    );
}
