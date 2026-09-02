package com.uestcfir.controller.user;


import com.uestcfir.config.EmailEncryptionService;
import com.uestcfir.enumeration.user.UserType;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.pojo.dto.*;
import com.uestcfir.pojo.entity.ImageUploadResult;
import com.uestcfir.pojo.entity.Reservations;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.entity.User;
import com.uestcfir.pojo.vo.LoginSuccessVo;
import com.uestcfir.pojo.vo.UserInfo;
import com.uestcfir.service.UserService;

import com.uestcfir.service.impl.OssFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Update;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import com.uestcfir.utils.JwtUtil;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户控制器
 *
 * @author ylshen
 * @since 1.0.0
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    UserService userService;

    @Autowired
    private OssFileStorageService ossFileStorageService;

    @Autowired
    private EmailEncryptionService emailEncryptionService;

    @PostMapping("/sendcode")
    /**
     * 发送验证码
     *
     * @param email 邮箱地址
     * @return 操作结果
     * @throws MailSendException 如果发送邮件失败
     */
    public Result sendCode(@RequestParam("email") String email) {
        log.info("send code to email: {}", email);

        return userService.sendCode(email);
    }

    /**
     * 注册操作
     */
    @PostMapping("/register")
    /**
     * 用户注册
     *
     * @param user 用户数据传输对象
     * @return 操作结果
     * @throws BusinessException 如果注册失败
     */
    public Result register(@RequestBody UserDto user) throws Exception {
        log.info("register user: {}", user);
        user.setUserType(UserType.USER.getCode());
        /**
         * 是否用手机号或邮箱作为用户名
         */

        return userService.register(user);
    }

    @PostMapping("/login/email")
    /**
     * 通过邮箱登录
     *
     * @param user 登录数据传输对象
     * @return 操作结果
     * @throws BusinessException 如果登录失败
     */
    public Result loginByEmail(@RequestBody LoginByEmailDto user) {
        log.info("login user: {}", user);
        User userEntity = userService.loginbyEmail(user);


        Map<String, Object> claims = new HashMap<>();
        claims.put("UserId", userEntity.getUserId());
        claims.put("UserType", userEntity.getUserType());
        String token = JwtUtil.generateToken(claims);
        log.info("generate token: {}", token);
        LoginSuccessVo loginSuccessVo = new LoginSuccessVo();
        loginSuccessVo.setToken(token);
        loginSuccessVo.setUserName(userEntity.getUsername());
        loginSuccessVo.setUserType(UserType.getByCode(userEntity.getUserType()).getDescription());
        loginSuccessVo.setAvatarUrl(userEntity.getThumbnailUrl());
        return Result.success(loginSuccessVo);
    }

    @PostMapping("/login/password")
    /**
     * 通过密码登录
     *
     * @param user 登录数据传输对象
     * @return 操作结果
     * @throws BusinessException 如果登录失败
     */
    public Result loginByPassword(@RequestBody(required = false) LoginByPasswordDto user) {
        if (user == null) {
            return Result.fail("请求体不能为空");
        }
        log.info("login user: {}", user);
        User userEntity = userService.loginByPassword(user);
        if (userEntity == null) {
            return Result.fail("用户名或密码错误");
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("UserId", userEntity.getUserId());
        claims.put("UserType", userEntity.getUserType());
        String token = JwtUtil.generateToken(claims);
        log.info("generate token: {}", token);
        LoginSuccessVo loginSuccessVo = new LoginSuccessVo();
        loginSuccessVo.setToken(token);
        loginSuccessVo.setUserName(userEntity.getUsername());
        loginSuccessVo.setUserType(UserType.getByCode(userEntity.getUserType()).getDescription());
        loginSuccessVo.setAvatarUrl(userEntity.getThumbnailUrl());
        return Result.success(loginSuccessVo);
    }


    @PostMapping("/changepassword")
    /**
     * 修改密码
     *
     * @param ChangePasswordDto 用户数据传输对象
     * @return 操作结果
     * @throws BusinessException 如果修改失败
     */
    public Result changePassword(@RequestBody ChangePasswordDto changePasswordDto) {

        log.info("change password: {}", changePasswordDto);
        return userService.changePassword(changePasswordDto);
    }

    /**
     * 上传头像
     *
     * @param file 头像文件
     * @return 操作结果
     * @throws BusinessException 如果修改失败
     */
    @PostMapping("/uploadavatar")
    public Result uploadAvatar(@RequestPart("file") MultipartFile file, @RequestHeader("Authorization") String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        log.info("upload avatar for user: {}, file: {}", userId, file);
        try {
            ImageUploadResult imageresult = ossFileStorageService.storeUserAvatar(file, userId);
            return Result.success(imageresult);
        } catch (IOException e) {
            log.error("上传头像失败: {}", e.getMessage(), e);
            return Result.fail("上传头像失败: " + e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result getUserInfo(@RequestHeader("Authorization") String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        User user = userService.getUserInfo(userId);
        String decryptedEmail = emailEncryptionService.decryptEmail(user.getEmail());
        //解密之后
        UserInfo userInfo = new UserInfo(user.getUserId(),user.getUsername(),user.getRealName(),decryptedEmail,user.getPhone(),user.getIntegral(),user.getThumbnailUrl());


        return Result.success(userInfo);
    }

    @PutMapping("/update")
    public Result updateUserInfo(@RequestHeader("Authorization") String authHeader, @RequestBody UpdateUserDto user) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        userService.updateUserInfo(userId, user);
        return Result.success();
    }


}