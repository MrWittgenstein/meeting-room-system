package com.uestcfir.controller.approver;

import com.uestcfir.auth.SessionService;
import com.uestcfir.auth.SessionUser;
import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.auth.RolePermissionRegistry;
import com.uestcfir.config.EmailEncryptionService;
import com.uestcfir.enumeration.user.UserType;
import com.uestcfir.pojo.dto.LoginByEmailDto;
import com.uestcfir.pojo.dto.LoginByPasswordDto;
import com.uestcfir.pojo.dto.UserDto;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.entity.User;
import com.uestcfir.pojo.vo.LoginSuccessVo;
import com.uestcfir.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/approver")
public class ApproverController {
    private final UserService userService;
    private final SessionService sessionService;

    @PostMapping("/sendcode")
    public Result sendCode(@RequestParam("email") String email) {
        return userService.sendCode(email);
    }

    @PostMapping("/register")
    public Result register(@RequestBody UserDto user) throws Exception {
        user.setUserType(UserType.APPROVER.getCode());
        return userService.register(user);
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginByEmailDto user) {
        User userEntity = userService.loginbyEmail(user);
        return Result.success(toLoginResponse(userEntity));
    }

    @PostMapping("/login/password")
    public Result loginByPassword(@RequestBody LoginByPasswordDto user) {
        User userEntity = userService.loginByPassword(user);
        if (userEntity == null) {
            return Result.fail("用户名或密码错误");
        }
        return Result.success(toLoginResponse(userEntity));
    }

    @PostMapping("/logout")
    public Result logout() {
        SessionUser current = CurrentUserContext.requireUser();
        sessionService.invalidate(current.getSessionId());
        return Result.success();
    }

    private LoginSuccessVo toLoginResponse(User user) {
        SessionUser sessionUser = sessionService.create(user);
        LoginSuccessVo response = new LoginSuccessVo();
        response.setToken(sessionUser.getSessionId());
        response.setSessionId(sessionUser.getSessionId());
        response.setUserName(user.getUsername());
        response.setUserType(RolePermissionRegistry.displayUserType(sessionUser.getRole(), user.getUserType()));
        response.setAvatarUrl(user.getThumbnailUrl());
        response.setRole(sessionUser.getRole());
        response.setPermissions(sessionUser.getPermissions());
        return response;
    }
}
