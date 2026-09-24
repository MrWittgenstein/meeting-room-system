package com.uestcfir.service;

import com.uestcfir.mapper.ReservationMapper;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ReservationPermissionSqlTest {
    @Test
    void actualMapperEnforcesStatusOwnershipRoomTimeAndCancellation() throws Exception {
        var dataSource = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
        config.addMapper(ReservationMapper.class);
        try (SqlSession session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            var connection = session.getConnection();
            connection.createStatement().execute("CREATE TABLE reservations(user_id INT, room_id INT, reserve_date DATE, start_time TIME, end_time TIME, status INT)");
            connection.createStatement().execute("INSERT INTO reservations VALUES(18,14,DATE '2026-09-23',TIME '10:00:00',TIME '11:00:00',4)");
            var mapper = session.getMapper(ReservationMapper.class);
            LocalDate date = LocalDate.of(2026, 9, 23);
            for (int repetition = 0; repetition < 2; repetition++) {
                for (int status : new int[]{0,1,2,3,4,5}) {
                    connection.createStatement().execute("UPDATE reservations SET status=" + status);
                    session.clearCache();
                    assertEquals(status == 3 || status == 4, mapper.existsActiveReservation(18,14,date,LocalTime.of(10,30)));
                }
                connection.createStatement().execute("UPDATE reservations SET status=4");
                session.clearCache();
                assertFalse(mapper.existsActiveReservation(19,14,date,LocalTime.of(10,30)));
                assertFalse(mapper.existsActiveReservation(18,15,date,LocalTime.of(10,30)));
                assertFalse(mapper.existsActiveReservation(18,14,date,LocalTime.of(9,59,59)));
                assertTrue(mapper.existsActiveReservation(18,14,date,LocalTime.of(10,0)));
                assertTrue(mapper.existsActiveReservation(18,14,date,LocalTime.of(10,59,59)));
                assertFalse(mapper.existsActiveReservation(18,14,date,LocalTime.of(11,0)));
                assertFalse(mapper.existsActiveReservation(18,14,date,LocalTime.of(11,0,1)));
            }
            connection.createStatement().execute("DELETE FROM reservations");
            session.clearCache();
            assertFalse(mapper.existsActiveReservation(18,14,date,LocalTime.of(10,30)));
        }
    }
}
