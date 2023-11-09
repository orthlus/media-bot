package main;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.starter.SpringWebhookBot;

@Component
public class Bot extends SpringWebhookBot {
	private final BotConfig config;
	private final BotHandler botHandler;
	private final String webhookUrl;

	public Bot(@Value("${webhook.url}") String webhookUrl,
			   BotConfig config,
			   BotHandler botHandler) {
		super(new SetWebhook(webhookUrl), config.getToken());
		this.config = config;
		this.webhookUrl = webhookUrl;
		this.botHandler = botHandler;
	}

	@Override
	public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
		return botHandler.onWebhookUpdate(update);
	}

	@Override
	public String getBotPath() {
		return webhookUrl;
	}

	@Override
	public String getBotUsername() {
		return config.getNickname();
	}
}
