package main;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class BotHandler {
	private final InstagramService instagram;
	private final YouTubeService youTube;
	private final TikTokController tikTok;

	public BotApiMethod<?> onWebhookUpdate(Update update) {
		return null;
	}
}
