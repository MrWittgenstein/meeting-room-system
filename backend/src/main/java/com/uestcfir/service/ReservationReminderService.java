package com.uestcfir.service;

import com.uestcfir.config.EmailEncryptionService;
import com.uestcfir.client.NotificationServiceClient;
import com.uestcfir.enumeration.reservation.ReservationStatus;
import com.uestcfir.mapper.ReservationMapper;
import com.uestcfir.mapper.UserMapper;
import com.uestcfir.pojo.entity.Reservations;
import com.uestcfir.pojo.entity.User;
import com.uestcfir.websocket.WebSocketServer;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
public class ReservationReminderService {
    @Autowired
    private ReservationMapper reservationMapper;
    @Autowired
    private EmailSender emailSender;

    @Autowired
    private WebSocketServer webSocketService;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EmailEncryptionService emailEncryptionService;
    @Autowired
    private NotificationServiceClient notificationServiceClient;



    @XxlJob("reservationReminderDispatch")
    public void checkUpcomingReservations() {
        LocalTime now = LocalTime.now();
        LocalTime threshold = now.plusMinutes(15); // 检查未来15分钟内的预约

        List<Reservations> upcomingReservations = reservationMapper.findUpcomingReservations(now, threshold);

        for (Reservations reservation : upcomingReservations) {
            boolean deliveredRemotely = false;
            try {
                // Notification delivery is owned by notification-service when enabled.
                deliveredRemotely = notificationServiceClient.sendMeetingReminder(reservation);
                if (!notificationServiceClient.isEnabled()) {
                    webSocketService.sendMeetingReminder(reservation.getUserId(), reservation);
                }

                log.info("成功发送会议提醒: {}", reservation.getReservationId());

            } catch (Exception e) {
                log.error("发送会议提醒失败: {}");
            }
            reservation.setStatus(ReservationStatus.USING.getCode()); // 更新状态为正在使用
            reservationMapper.updateReservationStatus(reservation);
            // The legacy email path is available only for explicitly disabled local runs.
            if (!notificationServiceClient.isEnabled()) {
                User user = userMapper.selectByUserId(reservation.getUserId());
                String email = emailEncryptionService.decryptEmail(user.getEmail());
                emailSender.sendEmail(email, "会议提醒", "您预约的会议马上开始开始");
            }

        }
    }


}
