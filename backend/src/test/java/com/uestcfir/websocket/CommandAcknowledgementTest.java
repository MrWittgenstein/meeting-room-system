package com.uestcfir.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uestcfir.exception.BusinessException;
import com.uestcfir.pojo.dto.IotDeviceCommandRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommandAcknowledgementTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private IotDeviceWebSocketHandler handler(String status, boolean foreignConnection) throws Exception {
        var handler = new IotDeviceWebSocketHandler();
        ReflectionTestUtils.setField(handler, "objectMapper", mapper);
        ReflectionTestUtils.setField(handler, "ackTimeoutMillis", 20L);
        var session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("connection-1");
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(Map.of("deviceId", "raspi-01"));
        handler.afterConnectionEstablished(session);
        var foreign = mock(WebSocketSession.class);
        lenient().when(foreign.getId()).thenReturn("connection-2");
        lenient().when(foreign.getAttributes()).thenReturn(Map.of("deviceId", "raspi-02"));
        doAnswer(invocation -> {
            if (status != null) {
                var command = mapper.readTree(((TextMessage) invocation.getArgument(0)).getPayload());
                var ack = Map.of("type", "COMMAND_ACK", "commandId", command.get("commandId").asText(),
                        "deviceId", "raspi-01", "status", status);
                handler.handleTextMessage(foreignConnection ? foreign : session, new TextMessage(mapper.writeValueAsString(ack)));
            }
            return null;
        }).when(session).sendMessage(any(TextMessage.class));
        return handler;
    }

    private IotDeviceCommandRequest command() {
        var command = new IotDeviceCommandRequest();
        command.setCommand("set_device_state");
        command.setTarget("light");
        command.setValue(true);
        return command;
    }

    @Test
    void acceptsOnlyExplicitSuccessfulExecution() throws Exception {
        assertEquals("success", handler("success", false).sendCommand("raspi-01", command()).getStatus());
        assertEquals(502, assertThrows(BusinessException.class, () -> handler("noop", false).sendCommand("raspi-01", command())).getStatus());
    }

    @RepeatedTest(5)
    void deviceErrorNeverBecomesSuccess() throws Exception {
        var handler = handler("error", false);
        assertEquals(502, assertThrows(BusinessException.class, () -> handler.sendCommand("raspi-01", command())).getStatus());
    }

    @RepeatedTest(5)
    void missingAckTimesOut() throws Exception {
        var handler = handler(null, false);
        assertEquals(504, assertThrows(BusinessException.class, () -> handler.sendCommand("raspi-01", command())).getStatus());
    }

    @Test
    void anotherConnectionCannotAcknowledgeCommand() throws Exception {
        var handler = handler("success", true);
        assertEquals(504, assertThrows(BusinessException.class, () -> handler.sendCommand("raspi-01", command())).getStatus());
    }
}
