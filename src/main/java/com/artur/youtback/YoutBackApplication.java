package com.artur.youtback;

import com.artur.youtback.config.ObjectStorageConfig;
import com.artur.youtback.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = "com.artur")
@EnableTransactionManagement
@EnableConfigurationProperties(ObjectStorageConfig.class)
@Component
public class YoutBackApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(YoutBackApplication.class, args);
	}

	@Bean
	CommandLineRunner commandLineRunner(UserService userService){
		return args -> {
			//
		};
	}
}
