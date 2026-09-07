package com.uestcfir.controller.user;

import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.auth.RolePermissionRegistry;
import com.uestcfir.auth.SessionService;
import com.uestcfir.auth.SessionUser;
import com.uestcfir.config.EmailEncryptionService;
import com.uestcfir.enumeration.user.UserType;
import com.uestcfir.pojo.dto.ChangePasswordDto;
import com.uestcfir.pojo.dto.LoginByEmailDto;
import com.uestcfir.pojo.dto.LoginByPasswordDto;
import com.uestcfir.pojo.dto.UpdateUserDto;
import com.uestcfir.pojo.dto.UserDto;
import com.uestcfir.pojo.entity.ImageUploadResult;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.entity.User;
import com.uestcfir.pojo.vo.LoginSuccessVo;
import com.uestcfir.pojo.vo.UserInfo;
import com.uestcfir.service.UserService;
import com.uestcfir.service.impl.OssFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Slf4j
public class UserController {
    private final UserService userService;
    private final OssFileStorageService ossFileStorageService;
    private final EmailEncryptionService emailEncryptionService;
    private final SessionService sessionService;

    @PostMapping("/sendcode")
    public Result sendCode(@RequestParam("email") String email) {
        return userService.sendCode(email);
    }

    @PostMapping("/register")
    public Result register(@RequestBody UserDto user) throws Exception {
        user.setUserType(UserType.USER.getCode());
        return userService.register(user);
    }

    @PostMapping("/login/email")
    public Result loginByEmail(@RequestBody LoginByEmailDto user) {
        User userEntity = userService.loginbyEmail(user);
        return Result.success(toLoginResponse(userEntity));
    }

    @PostMapping("/login/password")
    public Result loginByPassword(@RequestBody(required = false) LoginByPasswordDto user) {
        if (user == null) {
            return Result.fail("请求体不能为空");
        }
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

    @PostMapping("/changepassword")
    public Result changePassword(@RequestBody ChangePasswordDto changePasswordDto) {
        return userService.changePassword(changePasswordDto);
    }

    @PostMapping("/uploadavatar")
    public Result uploadAvatar(@RequestPart("file") MultipartFile file) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.USER_UPDATE_SELF);
        Integer userId = CurrentUserContext.requireUserId();
        log.info("upload avatar for user: {}, fileName: {}", userId, file == null ? null : file.getOriginalFilename());
        try {
            ImageUploadResult imageResult = ossFileStorageService.storeUserAvatar(file, userId);
            return Result.success(imageResult);
        } catch (IOException e) {
            log.error("upload avatar failed: {}", e.getMessage(), e);
            return Result.fail("上传头像失败");
        }
    }

    @GetMapping("/info")
    public Result getUserInfo() {
        CurrentUserContext.requirePermission(RolePermissionRegistry.USER_READ_SELF);
        User user = userService.getUserInfo(CurrentUserContext.requireUserId());
        String decryptedEmail = emailEncryptionService.decryptEmail(user.getEmail());
        UserInfo userInfo = new UserInfo(user.getUserId(), user.getUsername(), user.getRealName(),
                decryptedEmail, user.getPhone(), user.getIntegral(), user.getThumbnailUrl());
        return Result.success(userInfo);
    }

    @PutMapping("/update")
    public Result updateUserInfo(@RequestBody UpdateUserDto user) {
        CurrentUserContext.requirePermission(RolePermissionRegistry.USER_UPDATE_SELF);
        userService.updateUserInfo(CurrentUserContext.requireUserId(), user);
        return Result.success();
    }

    private LoginSuccessVo toLoginResponse(User user) {
        SessionUser sessionUser = sessionService.create(user);
        LoginSuccessVo response = new LoginSuccessVo();
        // Keep the old field so existing frontends can continue sending Bearer.
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
