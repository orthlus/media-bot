package main;

import art.aelaort.SpringLongPollingBot;
import art.aelaort.TelegramInit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.TelegramUrl;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.function.Supplier;

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
	public TelegramClient telegramClient(Supplier<TelegramUrl> telegramUrlSupplier) {
		return new OkHttpTelegramClient(botToken, telegramUrlSupplier.get());
	}

	@Bean
	public Supplier<TelegramUrl> telegramUrlSupplier() {
		if (schema.equals("none")) {
			return () -> TelegramUrl.DEFAULT_URL;
		}
		return () -> new TelegramUrl(schema, host, port);
	}

	@Bean
    public TelegramBotsLongPollingApplication telegramBotsApplication() {
        return new TelegramBotsLongPollingApplication();
    }

	@Bean
	public TelegramInit telegramInit(List<SpringLongPollingBot> bots) {
		return new TelegramInit(telegramBotsApplication(), bots, telegramUrlSupplier());
	}
}
