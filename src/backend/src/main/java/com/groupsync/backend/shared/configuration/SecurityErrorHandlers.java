package com.groupsync.backend.shared.configuration;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import tools.jackson.databind.ObjectMapper;
import com.groupsync.backend.shared.exception.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SecurityErrorHandlers {
    private final ObjectMapper objectMapper;

    public SecurityErrorHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> write(response, 401,
            new ApiError("UNAUTHENTICATED", "Authentication is required."));
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> write(response, 403,
            new ApiError("FORBIDDEN", "You do not have permission to perform this action."));
    }

    private void write(HttpServletResponse response, int status, ApiError error) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
