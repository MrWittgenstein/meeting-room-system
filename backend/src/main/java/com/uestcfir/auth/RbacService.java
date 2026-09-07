package com.uestcfir.auth;

import com.uestcfir.exception.BusinessException;
import com.uestcfir.enumeration.user.UserType;
import com.uestcfir.mapper.RbacMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RbacService {
    private final RbacMapper rbacMapper;

    @Transactional
    public AuthorizationSnapshot resolve(Integer userId, Integer legacyUserType) {
        List<String> roles = rbacMapper.findRoleCodes(userId);
        if (roles == null || roles.isEmpty()) {
            assignLegacyRole(userId, legacyUserType);
            roles = rbacMapper.findRoleCodes(userId);
        } else if (isLegacyAdministrator(legacyUserType) && "user".equalsIgnoreCase(roles.get(0))) {
            // Existing databases may have user_role=user while the legacy
            // user_type column was changed to an administrator value.
            // Synchronize only this stale default role; explicit admin roles
            // remain authoritative.
            rbacMapper.deleteUserRoles(userId);
            assignLegacyRole(userId, legacyUserType);
            roles = rbacMapper.findRoleCodes(userId);
        }
        if (roles == null || roles.isEmpty()) {
            throw new BusinessException("用户尚未分配角色");
        }
        Set<String> permissions = rbacMapper.findPermissionCodes(userId);
        return new AuthorizationSnapshot(roles.get(0),
                permissions == null ? Set.of() : new LinkedHashSet<>(permissions));
    }

    @Transactional
    public void replaceLegacyRole(Integer userId, Integer legacyUserType) {
        rbacMapper.deleteUserRoles(userId);
        assignLegacyRole(userId, legacyUserType);
    }

    @Transactional
    public void replaceRole(Integer userId, String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new BusinessException("角色不能为空");
        }
        rbacMapper.deleteUserRoles(userId);
        if (rbacMapper.insertUserRole(userId, roleCode.trim()) == 0) {
            throw new BusinessException("角色不存在或已禁用: " + roleCode);
        }
    }

    private void assignLegacyRole(Integer userId, Integer legacyUserType) {
        String roleCode = RolePermissionRegistry.roleOf(legacyUserType);
        if (rbacMapper.insertUserRole(userId, roleCode) == 0) {
            throw new BusinessException("角色不存在: " + roleCode);
        }
    }

    private boolean isLegacyAdministrator(Integer legacyUserType) {
        return UserType.ADMIN.getCode().equals(legacyUserType)
                || UserType.APPROVER.getCode().equals(legacyUserType);
    }

    public record AuthorizationSnapshot(String primaryRole, Set<String> permissions) {
    }
}
