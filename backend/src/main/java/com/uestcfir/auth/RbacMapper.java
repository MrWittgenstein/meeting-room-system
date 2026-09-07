package com.uestcfir.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface RbacMapper {
    @Select("""
            SELECT DISTINCT r.role_code
            FROM user_role ur
            JOIN sys_role r ON r.role_id = ur.role_id AND r.enabled = 1
            WHERE ur.user_id = #{userId}
            ORDER BY r.role_code
            """)
    List<String> findRoleCodes(@Param("userId") Integer userId);

    @Select("""
            SELECT DISTINCT p.permission_code
            FROM user_role ur
            JOIN sys_role r ON r.role_id = ur.role_id AND r.enabled = 1
            JOIN role_permission rp ON rp.role_id = r.role_id
            JOIN sys_permission p ON p.permission_id = rp.permission_id AND p.enabled = 1
            WHERE ur.user_id = #{userId}
            ORDER BY p.permission_code
            """)
    Set<String> findPermissionCodes(@Param("userId") Integer userId);

    @Delete("DELETE FROM user_role WHERE user_id = #{userId}")
    int deleteUserRoles(@Param("userId") Integer userId);

    @Insert("""
            INSERT INTO user_role (user_id, role_id)
            SELECT #{userId}, role_id FROM sys_role WHERE role_code = #{roleCode} AND enabled = 1
            """)
    int insertUserRole(@Param("userId") Integer userId, @Param("roleCode") String roleCode);
}
