package cn.edu.cug.campuslostfound.interceptor;

import cn.edu.cug.campuslostfound.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 注意：OPTIONS 请求是跨域预检请求，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");

        if (token == null || token.isEmpty()) {
            response.setStatus(401); // 401：未登录
            return false;
        }

        try {
            Claims claims = JwtUtils.parseToken(token);
            String role = claims.get("role", String.class);

            // ！！！VIP 核心校验 ！！！
            if (!"ADMIN".equals(role)) {
                // 如果不是管理员，返回 403 (Forbidden: 拒绝访问)
                // 403的意思是：我知道你是谁，但你没有权限进这里！
                response.setStatus(403);
                return false;
            }

            // 如果是管理员，同样把 ID 存进去方便后续使用
            Long userId = Long.valueOf(claims.get("userId").toString());
            request.setAttribute("currentUserId", userId);
            return true;

        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }
}
