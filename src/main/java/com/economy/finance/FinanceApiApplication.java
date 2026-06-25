package com.economy.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.economy.finance.config.MailProperties;
import com.economy.finance.config.PasswordResetProperties;
import com.economy.finance.security.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, PasswordResetProperties.class, MailProperties.class})
public class FinanceApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinanceApiApplication.class, args);
	}

}
