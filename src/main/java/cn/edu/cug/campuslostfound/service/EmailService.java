package cn.edu.cug.campuslostfound.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // 从 application.yml 读取发件人邮箱
    @Value("${spring.mail.username}")
    private String fromEmail;

    // 简易内存缓存，用来存验证码。格式：{ "张三@qq.com" : "123456_时间戳" }
    private final Map<String, String> verificationCodes = new ConcurrentHashMap<>();

    // 功能 1：生成并发送验证码
    public void sendVerificationCode(String targetEmail) {
        // 生成 6 位随机数字
        String code = String.valueOf((int)((Math.random() * 9 + 1) * 100000));

        // 存入内存，拼接上当前的时间戳（用于判断是否过期）
        verificationCodes.put(targetEmail, code + "_" + System.currentTimeMillis());

        // 构建邮件内容并发送
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(targetEmail);
        message.setSubject("【校园失物招领】注册验证码");
        message.setText("欢迎注册！您的验证码是：" + code + "。该验证码在 5 分钟内有效，请勿泄露给他人。");

        mailSender.send(message);
    }

    // 功能 2：校验验证码是否正确
    public boolean verifyCode(String email, String inputCode) {
        String storedData = verificationCodes.get(email);
        if (storedData == null) {
            return false; // 根本没发过，或者服务器重启清空了
        }

        String[] parts = storedData.split("_");
        String storedCode = parts[0];
        long timestamp = Long.parseLong(parts[1]);

        // 判断是否超过 5 分钟 (5分钟 = 300,000毫秒)
        if (System.currentTimeMillis() - timestamp > 300000) {
            verificationCodes.remove(email); // 过期了就删掉
            return false;
        }

        // 判断验证码是否一致
        if (storedCode.equals(inputCode)) {
            verificationCodes.remove(email); // 验证成功，立马作废（防止重放攻击）
            return true;
        }
        return false;
    }
}