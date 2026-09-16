package com.uestcfir.service.impl;

import com.uestcfir.auth.RbacService;
import com.uestcfir.config.EmailEncryptionService;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.mapper.UserMapper;
import com.uestcfir.pojo.entity.User;
import com.uestcfir.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {
    private final EmailEncryptionService encryptionService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RbacService rbacService;

    @Override
    public User getUserById(Integer id) {
        return userMapper.selectUserById(id);
    }

    @Override
    public List<User> getAllUsers() {
        return userMapper.selectUsersWithPage(null, 0, 1000);
    }

    @Override
    @Transactional
    public boolean addUser(User user) throws Exception {
        String originalEmail = user.getEmail();
        String emailHash = encryptionService.computeEmailHash(originalEmail);
        if (userMapper.existsByEmailHash(emailHash)) {
            throw new BusinessException("邮箱已被注册");
        }
        user.setEmailHash(emailHash);
        user.setEmail(encryptionService.encryptEmail(originalEmail));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreateTime(user.getCreateTime() == null ? LocalDateTime.now() : user.getCreateTime());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(user.getStatus() == null ? 0 : user.getStatus());
        user.setIntegral(user.getIntegral() == null ? 0 : user.getIntegral());

        boolean inserted = userMapper.insertUser(user);
        if (inserted) {
            rbacService.replaceLegacyRole(user.getUserId(), user.getUserType());
        }
        return inserted;
    }

    @Override
    @Transactional
    public boolean updateUser(User user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            user.setEmailHash(encryptionService.computeEmailHash(user.getEmail()));
            user.setEmail(encryptionService.encryptEmail(user.getEmail()));
        }
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        boolean updated = userMapper.updateUser(user);
        if (updated && user.getUserType() != null) {
            rbacService.replaceLegacyRole(user.getUserId(), user.getUserType());
        }
        return updated;
    }

    @Override
    public boolean deleteUser(Integer id) {
        return userMapper.deleteUser(id);
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public List<User> getUsersByType(Integer userType) {
        return userMapper.selectByUserType(userType);
    }

    public boolean checkUsernameExists(String username) {
        return userMapper.existsByUsername(username);
    }

    public boolean checkEmailExists(String email) {
        return userMapper.existsByEmail(email);
    }

    public Long getTotalUserCount() {
        return userMapper.countTotalUsers();
    }

    public List<User> getUsersByPage(String keyword, int page, int size) {
        return userMapper.selectUsersWithPage(keyword, (page - 1) * size, size);
    }

    public Long getUserCount(String keyword) {
        return userMapper.countUsers(keyword);
    }
}
