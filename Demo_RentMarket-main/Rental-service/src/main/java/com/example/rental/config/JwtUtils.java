package com.example.rental.config;

import com.example.rental.exception.AppException;
import com.example.rental.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Tiện ích lấy thông tin người dùng hiện tại từ JWT trong SecurityContext.
 * Identity-service đặt username vào claim "sub" (subject) của JWT.
 */
@Component
public class JwtUtils {

    /**
     * Lấy username của người dùng đang đăng nhập từ claim "sub" trong JWT.
     *
     * @return username (String)
     * @throws AppException UNAUTHENTICATED nếu không có JWT hợp lệ
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return subject;
    }
}
