package main;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.TelegramUrl;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class TelegramConfig {
	@Value("${telegram.api.url.schema}")
	private String schema;
	@Value("${telegram.api.url.host}")
	private String host;
	@Value("${telegram.api.url.port}")
	private int port;
	@Value("${bot.token}")
	private String botToken;

	@Bean
	public TelegramClient telegramClient() {
		TelegramUrl telegramUrl = new TelegramUrl(schema, host, port);
		return new OkHttpTelegramClient(botToken, telegramUrl);
	}
}
