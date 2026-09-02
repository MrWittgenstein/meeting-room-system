package com.uestcfir.mapper;

import com.uestcfir.pojo.dto.UserDto;
import com.uestcfir.pojo.entity.Reservations;
import com.uestcfir.pojo.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    // 插入用户
    boolean insertUser(User user);

    // 检查手机号是否存在
    boolean checkPhone(@Param("phone") String phone);

    // 根据邮箱查询用户
    User selectByEmail(@Param("email") String email);

    // 根据用户ID查询用户
    User selectByUserId(@Param("userId") Integer userId);

    // 更新密码
    boolean updatePassword(@Param("email") String email, @Param("password") String password);

    // 分页查询用户列表
    List<User> selectUsersWithPage(@Param("keyword") String keyword,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    // 统计用户数量
    Long countUsers(@Param("keyword") String keyword);

    // 根据ID查询用户
    User selectUserById(@Param("userId") Integer userId);

    // 检查用户名是否存在
    boolean existsByUsername(@Param("username") String username);

    // 检查邮箱是否存在
    boolean existsByEmail(@Param("email") String email);

    // 更新用户信息
    boolean updateUser(User user);

    // 删除用户
    boolean deleteUser(@Param("userId") Integer userId);

    // 统计总用户数
    Long countTotalUsers();

    // 根据用户类型统计
    Long countUsersByType(@Param("userType") Integer userType);

    // 根据用户状态统计
    Long countUsersByStatus(@Param("status") Integer status);

    // 根据用户名查询用户
    User selectByUsername(@Param("username") String username);

    // 根据用户类型查询用户列表
    List<User> selectByUserType(@Param("userType") Integer userType);

    // 根据邮箱哈希查询用户
    User selectByEmailHash(@Param("emailHash") String emailHash);

    // 检查邮箱哈希是否存在
    boolean existsByEmailHash(@Param("emailHash") String emailHash);

    // 更新邮箱哈希
    int updateEmailHash(@Param("id") Long id, @Param("emailHash") String emailHash);

    // 更新加密邮箱
    int updateEncryptedEmail(@Param("id") Long id, @Param("encryptedEmail") String encryptedEmail);

    // 更新头像URL
    boolean updateAvatarUrl(Integer userId,String originalFileUrl,String thumbnailFileUrl);


}
