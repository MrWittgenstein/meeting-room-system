package com.uestcfir.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testLoginByPassword() throws Exception {
        mockMvc.perform(post("/user/login/password")
                .contentType("application/json")
                .content("{\"email\": \"test@example.com\", \"password\": \"password123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    public void testRegister() throws Exception {
        mockMvc.perform(post("/user/register")
                .contentType("application/json")
                .content("{\"username\": \"testuser\", \"realName\": \"Test User\", \"code\": \"123456\", \"password\": \"password123\", \"email\": \"test@example.com\", \"phone\": \"1234567890\", \"userType\": 1}"))
                .andExpect(status().isOk());
    }
}