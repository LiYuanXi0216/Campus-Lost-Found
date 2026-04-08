package cn.edu.cug.campuslostfound.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer
{
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        // 获取当前项目的绝对路径
        String path = System.getProperty("user.dir") + "/uploads/";

        // 如果 uploads 文件夹不存在，就自动创建一个
        File uploadDir = new File(path);
        if (!uploadDir.exists())
        {
            uploadDir.mkdirs();
        }

        // 核心：把网址 /uploads/** 映射到硬盘的 uploads 文件夹
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + path);
    }
}
