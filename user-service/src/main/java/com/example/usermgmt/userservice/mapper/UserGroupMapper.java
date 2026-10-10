package com.example.usermgmt.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.usermgmt.common.enums.UserStatus;
import com.example.usermgmt.userservice.entity.UserGroupEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserGroupMapper extends BaseMapper<UserGroupEntity> {

    @Select("""
        SELECT COUNT(1)
        FROM user_and_group ug
        INNER JOIN `user` u ON u.id = ug.user_id
        WHERE ug.group_id = #{groupId}
          AND u.status = #{activeStatus}
        """)
    Integer countMembers(@Param("groupId") String groupId, @Param("activeStatus") int activeStatus);

    default Integer countMembers(String groupId) {
        return countMembers(groupId, UserStatus.ACTIVE.getCode());
    }

    @Select("""
        SELECT ug.user_id,
               ug.group_id,
               ug.type,
               ug.created_time,
               u.name AS user_name,
               JSON_UNQUOTE(JSON_EXTRACT(u.extra_data, '$.avatar')) AS avatar
        FROM user_and_group ug
        INNER JOIN `user` u ON u.id = ug.user_id
        WHERE ug.group_id = #{groupId}
          AND u.status = #{activeStatus}
        ORDER BY ug.created_time ASC
        """)
    List<UserGroupMemberRow> selectGroupMembers(@Param("groupId") String groupId, @Param("activeStatus") int activeStatus);

    default List<UserGroupMemberRow> selectGroupMembers(String groupId) {
        return selectGroupMembers(groupId, UserStatus.ACTIVE.getCode());
    }

    @Select("""
        SELECT g.id,
               g.name
        FROM user_and_group ug
        INNER JOIN `group` g ON g.id = ug.group_id
        INNER JOIN `user` u ON u.id = ug.user_id
        WHERE ug.user_id = #{userId}
          AND u.status = #{activeStatus}
        ORDER BY g.create_time ASC
        """)
    List<UserGroupSimpleRow> selectUserGroups(@Param("userId") String userId, @Param("activeStatus") int activeStatus);

    default List<UserGroupSimpleRow> selectUserGroups(String userId) {
        return selectUserGroups(userId, UserStatus.ACTIVE.getCode());
    }

    @Delete("""
        DELETE ug
        FROM user_and_group ug
        INNER JOIN `user` u ON u.id = ug.user_id
        WHERE u.status <> #{activeStatus}
        """)
    int deleteInactiveUserMemberships(@Param("activeStatus") int activeStatus);

    class UserGroupMemberRow {
        public String userId;
        public String groupId;
        public String type;
        public java.time.LocalDateTime createdTime;
        public String userName;
        public String avatar;
    }

    class UserGroupSimpleRow {
        public String id;
        public String name;
    }
}
