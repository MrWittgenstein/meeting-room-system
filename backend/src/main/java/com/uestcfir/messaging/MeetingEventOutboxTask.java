package com.uestcfir.messaging;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "meeting.events", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MeetingEventOutboxTask {

    private final MeetingEventOutboxService outboxService;

    @XxlJob("meetingEventOutboxDispatch")
    public void dispatch() {
        outboxService.dispatchPending();
    }
}
