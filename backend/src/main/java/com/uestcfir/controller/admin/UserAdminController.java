package com.uestcfir.controller.admin;

import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.auth.RolePermissionRegistry;
import com.uestcfir.pojo.entity.Result;
import com.uestcfir.pojo.entity.User;
import com.uestcfir.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserAdminController {
    private final UserAdminService userAdminService;

    @GetMapping("/{id}")
    public User getUser(@PathVariable Integer id) {
        requireUserManage();
        return userAdminService.getUserById(id);
    }

    @GetMapping("/all")
    public List<User> getAllUsers() {
        requireUserManage();
        return userAdminService.getAllUsers();
    }

    @PostMapping("/add")
    public boolean addUser(@RequestBody User user) throws Exception {
        requireUserManage();
        return userAdminService.addUser(user);
    }

    @PutMapping("/update/{userId}")
    public Result updateUser(@PathVariable Integer userId, @RequestBody User user) {
        requireUserManage();
        user.setUserId(userId);
        boolean success = userAdminService.updateUser(user);
        return success ? Result.success("用户信息修改成功") : Result.fail("用户信息修改失败");
    }

    @DeleteMapping("/{id}")
    public boolean deleteUser(@PathVariable Integer id) {
        requireUserManage();
        return userAdminService.deleteUser(id);
    }

    @GetMapping("/username/{username}")
    public User getUserByUsername(@PathVariable String username) {
        requireUserManage();
        return userAdminService.getUserByUsername(username);
    }

    @GetMapping("/type/{Type}")
    public List<User> getUsersByType(@PathVariable Integer Type) {
        requireUserManage();
        return userAdminService.getUsersByType(Type);
    }

    private void requireUserManage() {
        CurrentUserContext.requirePermission(RolePermissionRegistry.USER_MANAGE);
    }
}
