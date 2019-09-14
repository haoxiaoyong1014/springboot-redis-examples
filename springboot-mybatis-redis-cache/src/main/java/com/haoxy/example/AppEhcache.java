package com.haoxy.example;

import com.haoxy.example.utils.SpringContextHolder;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

/**
 * Created by hxy on 2018/6/27.
 * E-mail:hxyHelloWorld@163.com
 * github:https://github.com/haoxiaoyong1014
 *
 */
@SpringBootApplication
@MapperScan("com.haoxy.example.mapper")
public class AppEhcache {

    public static void main(String[] args) {
        SpringApplication.run(AppEhcache.class, args);
    }
}
