package cn.edu.cug.campuslostfound.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class JwtUtils {

    // 秘钥（随便写一段复杂的字符串，绝对不能泄露给前端！）
    private static final String SECRET_KEY = "CampusLostFound_Super_Secret_Key_ABCD";
    // 过期时间：设置为 24 小时 (毫秒)
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

    /**
     * 制造手环：生成 Token
     * 我们把用户的 ID 和 Username 存进手环里
     */
    public static String generateToken(Long userId, String username, String role) {
        return Jwts.builder()
                .claim("userId", userId)       // 存入用户ID
                .claim("username", username)   // 存入用户名
                .claim("role", role)  // ！！！把角色也存进手环！！！
                .setIssuedAt(new Date())       // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 过期时间
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY) // 使用 HS256 算法和秘钥签名
                .compact();
    }

    /**
     * 查验手环：解析 Token
     * 如果手环是伪造的、或者过期了，这里会直接抛出异常 (Exception)
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }
}
