package com.suke.czx.authentication.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suke.czx.common.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @Description 认证失败处理类
 * @Date 23:11
 * @Author yzcheng90@qq.com
 **/
@Slf4j
public class TokenAuthenticationFailHandler implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        if (response.isCommitted()) {
            log.warn("Response already committed, cannot write authentication error. URI={}, error={}", request.getRequestURI(), authException.getMessage());
            return;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        PrintWriter writer = response.getWriter();
        writer.write(objectMapper.writeValueAsString(R.error(HttpStatus.UNAUTHORIZED.value(),authException.getMessage())));
    }
}