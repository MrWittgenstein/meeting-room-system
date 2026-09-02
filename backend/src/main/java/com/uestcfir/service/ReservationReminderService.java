package com.uestcfir.service;

import com.uestcfir.config.EmailEncryptionService;
import com.uestcfir.enumeration.reservation.ReservationStatus;
import com.uestcfir.mapper.ReservationMapper;
import com.uestcfir.mapper.UserMapper;
import com.uestcfir.pojo.entity.Reservations;
import com.uestcfir.pojo.entity.User;
import com.uestcfir.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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



    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void checkUpcomingReservations() {
        LocalTime now = LocalTime.now();
        LocalTime threshold = now.plusMinutes(15); // 检查未来15分钟内的预约

        List<Reservations> upcomingReservations = reservationMapper.findUpcomingReservations(now, threshold);

        for (Reservations reservation : upcomingReservations) {
            try {
                // 发送WebSocket消息
                webSocketService.sendMeetingReminder(reservation.getUserId(), reservation);

                log.info("成功发送会议提醒: {}", reservation.getReservationId());

            } catch (Exception e) {
                log.error("发送会议提醒失败: {}");
            }
            reservation.setStatus(ReservationStatus.USING.getCode()); // 更新状态为正在使用
            reservationMapper.updateReservationStatus(reservation);
            User user = userMapper.selectByUserId(reservation.getUserId());
            //解密邮箱
            String email= emailEncryptionService.decryptEmail(user.getEmail());

            try {

                emailSender.sendEmail(email, "会议提醒", "您预约的会议马上开始开始");
            }
            catch (Exception e) {
                throw new RuntimeException("发送邮件失败");
            }

        }
    }


}