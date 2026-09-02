package com.uestcfir.service.impl;

import com.uestcfir.config.EmailEncryptionService;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.mapper.UserMapper;
import com.uestcfir.pojo.entity.User;
import com.uestcfir.service.UserAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAdminServiceImpl implements UserAdminService {


    @Autowired
    EmailEncryptionService encryptionService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public User getUserById(Integer id) {
        // 调用你Mapper中实际存在的方法
        return userMapper.selectUserById(id);
    }

    @Override
    public List<User> getAllUsers() {
        // 使用分页方法获取所有用户，设置较大的size
        return userMapper.selectUsersWithPage(null, 0, 1000);
    }

    @Override
    public boolean addUser(User user) throws Exception {
        // 1. 从user对象中获取原始邮箱
        String originalEmail = user.getEmail();

        // 2. 计算邮箱哈希值
        String emailHash = encryptionService.computeEmailHash(originalEmail);

        // 3. 检查邮箱是否已注册
        if (userMapper.existsByEmailHash(emailHash)) {
            throw new BusinessException("邮箱已被注册");
        }

        // 4. 加密邮箱
        String encryptedEmail = encryptionService.encryptEmail(originalEmail);

        // 5. 设置加密字段到user对象
        user.setEmailHash(emailHash);
        user.setEmail(encryptedEmail);
        //加密密码，单向加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userMapper.insertUser(user);
    }

    @Override
    public boolean updateUser(User user) {
        //加密邮箱
        String enencryption = encryptionService.encryptEmail(user.getEmail());
        user.setEmail(enencryption);
        //加密密码
        String enpassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(enpassword);

        return userMapper.updateUser(user);
    }

    @Override
    public boolean deleteUser(Integer id) {

        return userMapper.deleteUser(id);
    }





    @Override
    public User getUserByUsername(String username) {
        // 你的Mapper中没有selectByUsername方法，需要添加或使用其他方式
        // 暂时使用分页查询来实现
        List<User> users = userMapper.selectUsersWithPage(username, 0, 1);
        return users.isEmpty() ? null : users.get(0);
    }

    @Override
    public List<User> getUsersByType(Integer userType) {
        // 你的Mapper中没有selectByUserType方法
        // 暂时使用分页查询所有，然后在Service层过滤
        List<User> allUsers = getAllUsers();
        return allUsers.stream()
                .filter(user -> userType.equals(user.getUserType()))
                .collect(java.util.stream.Collectors.toList());
    }





    // 新增方法：检查用户名是否存在
    public boolean checkUsernameExists(String username) {
        return userMapper.existsByUsername(username);
    }

    // 新增方法：检查邮箱是否存在
    public boolean checkEmailExists(String email) {
        return userMapper.existsByEmail(email);
    }

    // 新增方法：获取用户统计信息
    public Long getTotalUserCount() {
        return userMapper.countTotalUsers();
    }

    // 新增方法：分页查询用户
    public List<User> getUsersByPage(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        return userMapper.selectUsersWithPage(keyword, offset, size);
    }

    // 新增方法：获取用户数量
    public Long getUserCount(String keyword) {
        return userMapper.countUsers(keyword);
    }



}
