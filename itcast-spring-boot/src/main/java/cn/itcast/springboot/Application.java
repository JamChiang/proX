package cn.itcast.springboot;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//组合注解；在spring boot项目的引导类的上面添加；默认扫描当前包及其子包
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        //SpringApplication.run(Application.class, args);

        SpringApplication springApplication = new SpringApplication(Application.class);
        //不使用Banner
        springApplication.setBannerMode(Banner.Mode.OFF);
        springApplication.run(args);

    }
}
