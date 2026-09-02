package com.uestcfir.controller.admin;

import com.uestcfir.enumeration.user.UserType;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.mapper.UserMapper;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.entity.User;
import com.uestcfir.service.UserAdminService;
import com.uestcfir.service.UserService;
import com.uestcfir.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserAdminController {

    @Autowired
    private UserAdminService userAdminService;


    @Autowired
    UserMapper userMapper;

    @GetMapping("/{id}")
    public User getUser(@PathVariable Integer id,@RequestHeader("Authorization") String authHeader) {

        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType!= 0) {
            throw new BusinessException("只有管理员才能查看用户信息");
        }
        return userAdminService.getUserById(id);
    }

    @GetMapping("/all")
    public List<User> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType!= 0) {
            throw new BusinessException("只有管理员才能查看用户信息");
        }
        return userAdminService.getAllUsers();
    }

    @PostMapping("/add")
    public boolean addUser(@RequestBody User user,@RequestHeader("Authorization") String authHeader) throws Exception {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType!= 0) {
            throw new BusinessException("只有管理员才能添加用户");
        }
        return userAdminService.addUser(user);
    }

    @PutMapping("/update/{userId}")
    public Result updateUser(@PathVariable Integer userId,
                             @RequestBody User user,
                             @RequestHeader("Authorization") String authHeader) {

        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer adminType = userinfo[1];

        if (adminType != 0) {
            throw new BusinessException("只有管理员才能修改用户信息");
        }

        // 设置要修改的用户ID
        user.setUserId(userId);
        boolean success = userAdminService.updateUser(user);

        return success ? Result.success("用户信息修改成功") : Result.fail("用户信息修改失败");
    }

    @DeleteMapping("/{id}")
    public boolean deleteUser(@PathVariable Integer id,@RequestHeader("Authorization") String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType!= 0) {
            throw new BusinessException("只有管理员才能删除用户");
        }
        return userAdminService.deleteUser(id);
    }






    @GetMapping("/username/{username}")
    public User getUserByUsername(@PathVariable String username,@RequestHeader("Authorization") String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType!= 0) {
            throw new BusinessException("只有管理员才能查看用户信息");
        }
        return userAdminService.getUserByUsername(username);
    }

    @GetMapping("/type/{Type}")
    public List<User> getUsersByType(@PathVariable Integer Type,@RequestHeader("Authorization") String authHeader) {
        Integer[] userinfo = JwtUtil.validateToken(authHeader);
        Integer userId = userinfo[0];
        Integer userType = userinfo[1];
        if (userType!= 0) {
            throw new BusinessException("只有管理员才能查看用户信息");
        }
        return userAdminService.getUsersByType(Type);
    }








}

