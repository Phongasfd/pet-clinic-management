package com.petclinic.petservice.filter;

import com.petclinic.petservice.context.UserContext;
import com.petclinic.petservice.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-User-Name");
        String role = request.getHeader("X-User-Role");

        // The gateway sets these after validating the JWT (and strips client-sent copies).
        // Missing headers mean the request did not come through the gateway.
        if (isBlank(userId) || isBlank(role)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        UserContext context = UserContext.builder()
                .userId(userId)
                .username(username)
                .role(role)
                .build();
                
        UserContextHolder.setContext(context);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clearContext();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
