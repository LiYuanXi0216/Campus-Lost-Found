package cn.edu.cug.campuslostfound.interceptor;

import cn.edu.cug.campuslostfound.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getHeader("Authorization");

        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        try {
            Claims claims = JwtUtils.parseToken(token);

            // 优化：更安全的类型转换方式，防止 JWT 把数字解析成 Integer 导致转换 Long 报错
            Long userId = Long.valueOf(claims.get("userId").toString());

            request.setAttribute("currentUserId", userId);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }
}
