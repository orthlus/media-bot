package main;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Configuration
public class LongPollingBotsInit implements InitializingBean {
	private final List<TelegramLongPollingBot> bots;
	@Value("${telegram.api.url}")
	private String baseUrl;

	@Override
	public void afterPropertiesSet() throws Exception {
		TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
		for (TelegramLongPollingBot bot : bots) {
			api.registerBot(bot);
			log.info("bot {} registered", bot.getBotUsername());
		}
	}

	@Bean
	public DefaultBotOptions defaultBotOptions() {
		DefaultBotOptions defaultBotOptions = new DefaultBotOptions();
		defaultBotOptions.setBaseUrl(baseUrl);
		return defaultBotOptions;
	}
}
