package com.example.usermgmt.userservice.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.usermgmt.userservice.entity.UserGroupEntity;
import com.example.usermgmt.userservice.mapper.UserGroupMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TemporaryMemberCleanupTask {

    private final UserGroupMapper userGroupMapper;

    @XxlJob("temporaryMemberCleanup")
    public void cleanupExpiredTemporaryMembers() {
        List<UserGroupEntity> temporaryMembers = userGroupMapper.selectList(
            new QueryWrapper<UserGroupEntity>().eq("type", "temporary")
        );
        LocalDateTime now = LocalDateTime.now();
        for (UserGroupEntity relation : temporaryMembers) {
            if (relation.getExtension() == null || relation.getExtension().isBlank()) {
                continue;
            }
            try {
                LocalDateTime expireAt = LocalDateTime.parse(relation.getExtension());
                if (expireAt.isBefore(now)) {
                    userGroupMapper.delete(
                        new QueryWrapper<UserGroupEntity>()
                            .eq("user_id", relation.getUserId())
                            .eq("group_id", relation.getGroupId())
                    );
                }
            } catch (DateTimeParseException ignored) {
            }
        }
    }
}
