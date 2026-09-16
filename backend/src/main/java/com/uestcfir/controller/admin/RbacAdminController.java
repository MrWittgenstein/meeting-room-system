package com.uestcfir.controller.admin;

import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.mapper.RbacMapper;
import com.uestcfir.auth.RbacService;
import com.uestcfir.auth.RolePermissionRegistry;
import com.uestcfir.pojo.entity.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rbac")
public class RbacAdminController {
    private final RbacMapper rbacMapper;
    private final RbacService rbacService;

    @GetMapping("/users/{userId}")
    public Result getUserAuthorization(@PathVariable Integer userId) {
        requireManage();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roles", rbacMapper.findRoleCodes(userId));
        data.put("permissions", rbacMapper.findPermissionCodes(userId));
        return Result.success(data);
    }

    @PutMapping("/users/{userId}/role")
    public Result replaceUserRole(@PathVariable Integer userId, @RequestBody RoleRequest request) {
        requireManage();
        if (request == null || request.getRoleCode() == null) {
            return Result.fail("角色不能为空");
        }
        rbacService.replaceRole(userId, request.getRoleCode());
        return Result.success();
    }

    private void requireManage() {
        CurrentUserContext.requirePermission(RolePermissionRegistry.USER_MANAGE);
    }

    @Data
    public static class RoleRequest {
        private String roleCode;
    }
}
