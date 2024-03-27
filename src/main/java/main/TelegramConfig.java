package main;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;

@Configuration
public class TelegramConfig {
	@Value("${telegram.api.url}")
	private String baseUrl;
	@Bean
	public DefaultBotOptions defaultBotOptions() {
		DefaultBotOptions defaultBotOptions = new DefaultBotOptions();
		defaultBotOptions.setBaseUrl(baseUrl);
		return defaultBotOptions;
	}
}
