package com.uestcfir.service;

import com.uestcfir.pojo.dto.*;

import com.uestcfir.pojo.entity.User;
import com.uestcfir.pojo.entity.Result;

public interface UserService {

    public Result sendCode(String email);
    public Result register(UserDto user) throws Exception;

    public User loginbyEmail(LoginByEmailDto user);

    public User loginByPassword(LoginByPasswordDto user);

    Result changePassword(ChangePasswordDto user);

    User getUserInfo(Integer userId);

    void updateUserInfo(Integer userId,UpdateUserDto user);
}
