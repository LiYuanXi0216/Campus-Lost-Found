package cn.edu.cug.campuslostfound;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("cn.edu.cug.campuslostfound.mapper")

@SpringBootApplication
public class CampusLostFoundApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusLostFoundApplication.class, args);
    }

}
