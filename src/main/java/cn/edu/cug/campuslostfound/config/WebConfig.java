package cn.edu.cug.campuslostfound.config;

import cn.edu.cug.campuslostfound.interceptor.AdminInterceptor;
import cn.edu.cug.campuslostfound.interceptor.AuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration // 告诉 Spring Boot 这是一个配置类
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthenticationInterceptor authInterceptor;
    @Autowired
    private AdminInterceptor adminInterceptor;

    /**
     * 功能 1：配置拦截器规则 (本次新增)
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 普通大门保安 (原来的代码保持不变)
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/users/login", "/api/users/register", "/api/users/send-code")
                .excludePathPatterns("/api/admin/**") // ！！把 admin 的路径从普通保安这里排除，交给内场保安管！！
                .excludePathPatterns("/api/posts/search", "/api/posts/type/**")
                .excludePathPatterns("/api/buildings")
                .excludePathPatterns("/uploads/**");


        // 2. VIP 内场保安 (新增代码)
        // 他只负责拦截 /api/admin 开头的所有请求
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**");
    }

    /**
     * 功能 2：配置静态资源映射 (之前写的，用于图片上传)
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = System.getProperty("user.dir") + "/uploads/";
        File uploadDir = new File(path);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + path);
    }
}
