package com.uestcfir.service.impl;



import com.uestcfir.auth.RbacService;
import com.uestcfir.config.EmailEncryptionService;
import com.uestcfir.enumeration.user.UserStatus;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.pojo.dto.*;
import com.uestcfir.pojo.entity.User;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.mapper.UserMapper;
import com.uestcfir.service.EmailSender;
import com.uestcfir.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 *
 * @author ylshen
 * @since 1.0.0
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    EmailEncryptionService encryptionService;
    @Autowired
    EmailSender emailSender;
    @Autowired
    UserMapper userMapper;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    RbacService rbacService;

    @Autowired
    EmailEncryptionService emailEncryptionService;


    @Override
    /**
     * 发送验证码
     *
     * @param email 邮箱地址
     * @return 操作结果
     * @throws MailSendException 如果发送邮件失败
     */
    public Result sendCode(String email) {
        // 检查请求频率（示例：60秒内只能请求一次）
        String cacheKey ="code_request_" +email;
        Object lastRequestTimeObj = redisTemplate.opsForValue().get(cacheKey);
        if (lastRequestTimeObj != null) {
            Long lastRequestTime = (Long) lastRequestTimeObj;
            if (System.currentTimeMillis() - lastRequestTime < 60000) {
                throw new BusinessException("请求过于频繁，请稍后再试");
            }
        }
        // 记录当前请求时间
        redisTemplate.opsForValue().set(cacheKey, System.currentTimeMillis(), 60, TimeUnit.SECONDS);
        log.info("Sending code to email: {}",email);
        String normal_email=emailSender.normalizeEmail(email);
        Random random = new Random();
        int code =random.nextInt(900000)+100000;//生成一个6位数的随机验证码
        String codeStr=""+code;//将验证码转换为字符串
        try {
            emailSender.sendEmail(normal_email, "Verification code", codeStr);
            redisTemplate.opsForValue().set(email, codeStr, 5, TimeUnit.MINUTES); // 存储验证码
            return Result.success();
        }


        catch (MailSendException e){
            log.error("Failed to send email: {}", e.getMessage());
            return Result.fail("邮件发送失败，请检查邮箱地址是否正确");
        }

    }

    @Override
    /**
     * 用户注册
     *
     * @param user 用户数据传输对象
     * @return 操作结果
     * @throws BusinessException 如果注册失败
     */
    @Transactional
    public Result register(UserDto user) throws Exception {
        log.info("Registering user: {}", user);
        //加密密码
        String password = passwordEncoder.encode(user.getPassword());
        // 计算邮箱哈希
        String emailHash = encryptionService.computeEmailHash(user.getEmail());

        // 检查邮箱是否已注册（使用哈希值检查）
        if (userMapper.existsByEmailHash(emailHash)) {
            return Result.fail("邮箱已被注册");
        }
        //获取原始邮箱
        String oriemail = user.getEmail();

        //加密邮箱
        String encryptedEmail = encryptionService.encryptEmail(user.getEmail());

        User finaluser=new User(null,user.getUsername(),user.getRealName(),password, encryptedEmail,emailHash, user.getPhone(), user.getUserType(),0,0, LocalDateTime.now(), LocalDateTime.now());
        String code=(String)redisTemplate.opsForValue().get(oriemail);
        if (code==null){
            return Result.fail("验证码已过期或不存在");
        }
        if (!code.equals(user.getCode())) {
            return Result.fail("验证码错误");
        }
        //if (userMapper.checkPhone(user.getPhone())){
        //    return Result.fail("Phone already exists");
        //}
        //if (userMapper.checkEmail(user.getEmail())){
        //    return Result.fail("Email already exists");
        //}
        if (userMapper.insertUser(finaluser)){
            rbacService.replaceLegacyRole(finaluser.getUserId(), finaluser.getUserType());
            redisTemplate.delete(oriemail);
            return Result.success();
        }
            return Result.fail("注册失败，请检查");
    }

    @Override
    /**
     * 通过邮箱登录
     *
     * @param user 登录数据传输对象
     * @return 用户信息
     * @throws BusinessException 如果登录失败
     */
    public User loginbyEmail(LoginByEmailDto user) {
        log.info("Login user: {}", user);
        String email_Hash = encryptionService.computeEmailHash(user.getEmail());

        User finaluser = userMapper.selectByEmailHash(email_Hash);
        if (finaluser == null) {
            throw new BusinessException("邮箱或密码错误");
        }
        if(finaluser.getStatus()== UserStatus.FROZEN.getCode()){
            throw new BusinessException("用户已被冻结");
        }



        // 3. 解密得到原始邮箱，用于验证码验证
        String originalEmail = encryptionService.decryptEmail(user.getEmail());
        String code=(String)redisTemplate.opsForValue().get(originalEmail);

        if (code==null){
            throw new BusinessException("验证码已过期或不存在");
        }
        if (!code.equals(user.getCode())) {
            throw new BusinessException("验证码错误");
        }

        /**
        if (finaluser == null) {
            return Result.fail("User not found");
        }
        if (finaluser.getPassword().equals(user.getPassword())) {
            return Result.success(finaluser);
        }
        return Result.fail("Password is incorrect");
    }
         **/
        log.info("用户 {} 验证码登录成功", finaluser.getUsername());
        return finaluser;}

    @Override
    /**
     * 通过密码登录
     *
     * @param user 登录数据传输对象
     * @return 用户信息
     * @throws BusinessException 如果登录失败
     */
    public User loginByPassword(LoginByPasswordDto user) {


        // 计算邮箱哈希并比较
        String emailHash = encryptionService.computeEmailHash(user.getEmail());
        User finaluser = userMapper.selectByEmailHash(emailHash);

        if (finaluser == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if(finaluser.getStatus()== UserStatus.FROZEN.getCode()){
            throw new BusinessException("用户已被冻结");
        }


        if (!passwordEncoder.matches(user.getPassword(),finaluser.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        log.info("用户 {} 密码登录成功", finaluser.getUsername());

        return finaluser;
    }

    @Override
    public Result changePassword(ChangePasswordDto user){
        if (user.getNewPassword() == null || user.getNewPassword().isEmpty()) {
            throw new BusinessException("新密码不能为空");
        }

        String code=(String)redisTemplate.opsForValue().get(user.getEmail());
        if (!code.equals(user.getCode())) {
            throw new BusinessException("验证码错误");
        }
        String password = passwordEncoder.encode(user.getNewPassword());
        String encryptedEmail =emailEncryptionService.encryptEmail(user.getEmail());
        if (userMapper.updatePassword(encryptedEmail,password)){
            return Result.success();
        }
        return Result.fail("密码重置失败，请检查邮箱是否注册");
    }

    @Override
    public User getUserInfo(Integer userId) {
        try {
            User user = userMapper.selectUserById(userId);
            return user;
        } catch (Exception e) {
            log.error("Failed to get user info: {}", e.getMessage());
            throw new BusinessException("获取用户信息失败");
        }
    }
    @Override
    public void updateUserInfo(Integer userId,UpdateUserDto user){
        try {
            User finaluser = new User();
            finaluser.setUsername(user.getUsername());
            finaluser.setRealName(user.getRealName());
            finaluser.setPhone(user.getPhone());
            finaluser.setUserId(userId);
            userMapper.updateUser(finaluser);
        }
        catch (Exception e) {
            log.error("Failed to update user info: {}", e.getMessage());
            throw new BusinessException("更新用户信息失败");
        }

    }


}


