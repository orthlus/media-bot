package art.aelaort.config;

import art.aelaort.SpringLongPollingBot;
import art.aelaort.TelegramClientBuilder;
import art.aelaort.TelegramInit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static art.aelaort.TelegramBots.createTelegramInit;
import static art.aelaort.TelegramBots.telegramUrlSupplier;

@Configuration
public class TelegramConfig {
	@Value("${telegram.api.url}")
	private String telegramUrl;
	@Value("${bot.token}")
	private String botToken;

	@Bean
	public TelegramClient telegramClient() {
		return TelegramClientBuilder.builder()
				.token(botToken)
				.telegramUrlSupplier(telegramUrlSupplier(telegramUrl))
				.build();
	}

	@Bean
	public TelegramInit telegramInit(List<SpringLongPollingBot> bots) {
		return createTelegramInit(bots);
	}
}
