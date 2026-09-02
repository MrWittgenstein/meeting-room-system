package com.uestcfir.controller.approver;


import com.uestcfir.enumeration.user.UserType;
import com.uestcfir.pojo.dto.LoginByEmailDto;
import com.uestcfir.pojo.dto.LoginByPasswordDto;
import com.uestcfir.pojo.dto.UserDto;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.entity.User;
import com.uestcfir.pojo.vo.LoginSuccessVo;
import com.uestcfir.service.UserService;
import com.uestcfir.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 审批人（管理员）控制器
 *
 * @author ylshen
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/approver")
public class ApproverController {

    @Autowired
    UserService userService;

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
        /**
         * 是否用手机号或邮箱作为用户名
         */
        user.setUserType(UserType.APPROVER.getCode());//
        return userService.register(user);
    }

    @PostMapping("/login")
    /**
     * 用户登录
     *
     * @param user 登录数据传输对象
     * @return 操作结果
     * @throws BusinessException 如果登录失败
     */
    public Result login(@RequestBody LoginByEmailDto user) {
        log.info("login user: {}", user);
        User userEntity = userService.loginbyEmail(user);
        if (userEntity == null) {
            return Result.fail("User not found");
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
        return Result.success(loginSuccessVo) ;
    }
//TODO: 重复登录问题，需要修改
    @PostMapping("/login/password")
    /**
     * 通过密码登录
     *
     * @param user 登录数据传输对象
     * @return 操作结果
     * @throws BusinessException 如果登录失败
     */
    public Result loginByPassword(@RequestBody LoginByPasswordDto user) {
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
        return Result.success(loginSuccessVo);
    }



}
