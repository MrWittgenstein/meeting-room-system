package com.uestcfir.service;

import com.uestcfir.auth.CurrentUserContext;
import com.uestcfir.auth.RolePermissionRegistry;
import com.uestcfir.auth.SessionUser;
import com.uestcfir.auth.RbacService;
import com.uestcfir.config.EmailEncryptionService;
import com.uestcfir.controller.admin.UserAdminController;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.mapper.UserMapper;
import com.uestcfir.pojo.entity.User;
import com.uestcfir.service.impl.UserAdminServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemAdminUserTest {
    @AfterEach void clear() { CurrentUserContext.clear(); }

    @Test void navigationIdentityRequiresSessionAndNeverReturnsSessionSecret() {
        var controller = new com.uestcfir.controller.user.UserController(null, null, null, null, null);
        assertEquals(401, assertThrows(BusinessException.class, controller::getSessionIdentity).getStatus());
        CurrentUserContext.set(SessionUser.builder().sessionId("private-secret").userId(1).username("admin")
                .role("super_admin").permissions(Set.of(RolePermissionRegistry.USER_MANAGE)).build());
        var identity = (java.util.Map<?, ?>) controller.getSessionIdentity().getData();
        assertEquals("super_admin", identity.get("role"));
        assertFalse(identity.containsKey("sessionId"));
        assertFalse(identity.containsValue("private-secret"));
    }

    @Test void anonymousAndNormalUserCannotManageUsers() {
        UserAdminService service = mock(UserAdminService.class);
        UserAdminController controller = new UserAdminController(service);
        assertEquals(401, assertThrows(BusinessException.class, controller::getAllUsers).getStatus());
        CurrentUserContext.set(SessionUser.builder().userId(1).role("user").permissions(Set.of()).build());
        assertEquals(403, assertThrows(BusinessException.class, controller::getAllUsers).getStatus());
        verifyNoInteractions(service);
    }

    @Test void rejectsInvalidInputBeforeDatabaseWrite() {
        UserAdminService service = mock(UserAdminService.class);
        UserAdminController controller = new UserAdminController(service);
        CurrentUserContext.set(SessionUser.builder().userId(1).role("super_admin")
                .permissions(Set.of(RolePermissionRegistry.USER_MANAGE)).build());
        User user = new User();
        user.setUsername("admin"); user.setEmail("invalid"); user.setPassword("long-password"); user.setUserType(2);
        assertEquals(400, assertThrows(BusinessException.class, () -> controller.addUser(user)).getStatus());
        verifyNoInteractions(service);
    }

    @Test void listDecryptsEmailWithoutReturningPasswordHash() {
        UserMapper mapper = mock(UserMapper.class);
        EmailEncryptionService encryption = mock(EmailEncryptionService.class);
        UserAdminServiceImpl service = new UserAdminServiceImpl(encryption, mapper, mock(PasswordEncoder.class), mock(RbacService.class));
        User user = new User(); user.setPassword("hash"); user.setEmailHash("email-hash"); user.setEmail("cipher");
        when(mapper.selectUsersWithPage(null, 0, 1000)).thenReturn(List.of(user));
        when(encryption.decryptEmail("cipher")).thenReturn("admin@example.com");
        User result = service.getAllUsers().get(0);
        assertNull(result.getPassword()); assertNull(result.getEmailHash()); assertEquals("admin@example.com", result.getEmail());
    }

    @Test void blankEditPasswordDoesNotOverwriteStoredPassword() {
        UserMapper mapper = mock(UserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserAdminServiceImpl service = new UserAdminServiceImpl(mock(EmailEncryptionService.class), mapper, encoder, mock(RbacService.class));
        User user = new User(); user.setUserId(3); user.setPassword("");
        service.updateUser(user);
        assertNull(user.getPassword()); verifyNoInteractions(encoder); verify(mapper).updateUser(user);
    }

    @Test void unreadableHistoricalEmailDoesNotBreakListOrMutateStoredValue() {
        UserMapper mapper = mock(UserMapper.class);
        EmailEncryptionService encryption = mock(EmailEncryptionService.class);
        UserAdminServiceImpl service = new UserAdminServiceImpl(encryption, mapper, mock(PasswordEncoder.class), mock(RbacService.class));
        User bad = new User(); bad.setUserId(8); bad.setEmail("old-cipher"); bad.setPassword("old-hash");
        User good = new User(); good.setUserId(20); good.setEmail("current-cipher");
        when(mapper.selectUsersWithPage(null, 0, 1000)).thenReturn(List.of(bad, good));
        when(encryption.decryptEmail("old-cipher")).thenThrow(new IllegalStateException("bad key"));
        when(encryption.decryptEmail("current-cipher")).thenReturn("admin@example.com");
        List<User> result = service.getAllUsers();
        assertEquals(2, result.size());
        assertNull(result.get(0).getEmail());
        assertNull(result.get(0).getPassword());
        assertTrue(((com.uestcfir.pojo.vo.AdminUserView) result.get(0)).isEmailUnavailable());
        assertEquals("admin@example.com", result.get(1).getEmail());
        assertEquals("old-cipher", bad.getEmail());
        assertEquals("old-hash", bad.getPassword());
        verify(mapper, never()).updateUser(any());
    }
}
