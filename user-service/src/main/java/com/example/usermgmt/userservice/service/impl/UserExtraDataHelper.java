package com.example.usermgmt.userservice.service.impl;

import com.example.usermgmt.userservice.dto.auth.UserExtraData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

final class UserExtraDataHelper {

    private UserExtraDataHelper() {
    }

    static UserExtraData parse(ObjectMapper objectMapper, String extraData) {
        return parse(objectMapper, extraData, UserExtraData.class);
    }

    static <T> T parse(ObjectMapper objectMapper, String extraData, Class<T> clazz) {
        if (!StringUtils.hasText(extraData)) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ignored) {
                return null;
            }
        }
        try {
            return objectMapper.readValue(extraData, clazz);
        } catch (JsonProcessingException exception) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    static String toJson(ObjectMapper objectMapper, Object extraData) {
        try {
            return objectMapper.writeValueAsString(extraData);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
