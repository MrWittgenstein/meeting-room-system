package com.example.usermgmt.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.usermgmt.userservice.dto.group.PermissionNodeResponse;
import com.example.usermgmt.userservice.entity.PermissionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PermissionMapper extends BaseMapper<PermissionEntity> {

    @Select("""
        SELECT menu_code
        FROM (
            SELECT p.code AS menu_code,
                   MIN(p.sort) AS menu_sort,
                   MIN(p.create_time) AS menu_create_time
            FROM permission p
            LEFT JOIN permission_and_user pu
              ON pu.permission_id = p.id AND pu.user_id = #{userId}
            LEFT JOIN user_and_group ug
              ON ug.user_id = #{userId}
            LEFT JOIN permission_and_group pg
              ON pg.group_id = ug.group_id AND pg.permission_id = p.id
            WHERE p.status = 1
              AND (pu.user_id IS NOT NULL OR pg.group_id IS NOT NULL)
            GROUP BY p.code
        ) menus
        ORDER BY menu_sort ASC, menu_create_time ASC
        """)
    List<String> selectEffectiveMenuNames(@Param("userId") String userId);

    @Select("""
        SELECT code
        FROM permission
        WHERE status = 1
        ORDER BY sort ASC, create_time ASC
        """)
    List<String> selectAllActivePermissionCodes();

    @Select("""
        SELECT p.id AS permId,
               p.code AS permCode,
               p.name AS permName,
               p.parent_id AS parentId,
               p.path,
               p.icon,
               p.sort,
               p.description,
               'user' AS grantType,
               '独立授权' AS source,
               DATE_FORMAT(pu.created_time, '%Y-%m-%d %H:%i:%s') AS grantedAt
        FROM permission p
        INNER JOIN permission_and_user pu ON pu.permission_id = p.id
        WHERE pu.user_id = #{userId}
        ORDER BY p.sort ASC, p.create_time ASC
        """)
    List<PermissionNodeResponse> selectDirectPermissions(@Param("userId") String userId);

    @Select("""
        SELECT p.id AS permId,
               p.code AS permCode,
               p.name AS permName,
               p.parent_id AS parentId,
               p.path,
               p.icon,
               p.sort,
               p.description,
               'group' AS grantType,
               g.name AS source,
               DATE_FORMAT(pg.created_time, '%Y-%m-%d %H:%i:%s') AS grantedAt
        FROM permission p
        INNER JOIN permission_and_group pg ON pg.permission_id = p.id
        INNER JOIN `group` g ON g.id = pg.group_id
        WHERE pg.group_id = #{groupId}
        ORDER BY p.sort ASC, p.create_time ASC
        """)
    List<PermissionNodeResponse> selectGroupPermissions(@Param("groupId") String groupId);

    @Select("""
        SELECT DISTINCT p.id AS permId,
               p.code AS permCode,
               p.name AS permName,
               p.parent_id AS parentId,
               p.path,
               p.icon,
               p.sort,
               p.create_time AS createTime,
               p.description,
               CASE WHEN pu.user_id IS NOT NULL THEN 'user' ELSE 'group' END AS grantType,
               CASE WHEN pu.user_id IS NOT NULL THEN '独立授权' ELSE g.name END AS source,
               DATE_FORMAT(COALESCE(pu.created_time, pg.created_time), '%Y-%m-%d %H:%i:%s') AS grantedAt
        FROM permission p
        LEFT JOIN permission_and_user pu
          ON pu.permission_id = p.id AND pu.user_id = #{userId}
        LEFT JOIN user_and_group ug
          ON ug.user_id = #{userId}
        LEFT JOIN permission_and_group pg
          ON pg.group_id = ug.group_id AND pg.permission_id = p.id
        LEFT JOIN `group` g
          ON g.id = pg.group_id
        WHERE pu.user_id IS NOT NULL OR pg.group_id IS NOT NULL
        ORDER BY p.sort ASC, p.create_time ASC
        """)
    List<PermissionNodeResponse> selectEffectivePermissions(@Param("userId") String userId);
}
