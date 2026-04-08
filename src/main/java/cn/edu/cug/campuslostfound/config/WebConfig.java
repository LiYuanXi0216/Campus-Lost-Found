package cn.edu.cug.campuslostfound.config;

// 这里是所有需要的导包（包括了上次图片上传和这次拦截器的依赖）
import cn.edu.cug.campuslostfound.interceptor.AuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration // 告诉 Spring Boot 这是一个配置类
public class WebConfig implements WebMvcConfigurer {

    // 注入我们刚刚写的保安（拦截器）
    @Autowired
    private AuthenticationInterceptor authInterceptor;

    /**
     * 功能 1：配置拦截器规则 (本次新增)
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/users/login", "/api/users/register")
                // 👇 注意看这行，我把末尾的 "/api/posts" 删掉了！
                .excludePathPatterns("/api/posts/search", "/api/posts/type/**")
                .excludePathPatterns("/uploads/**");
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
