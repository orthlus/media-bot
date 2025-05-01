package art.aelaort.service;

import art.aelaort.service.social.ig.InstagramHandler;
import art.aelaort.service.social.tiktok.TikTokHandler;
import art.aelaort.service.social.vk.VkHandler;
import art.aelaort.service.social.yt.YoutubeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.net.URI;

import static art.aelaort.utils.BotUtils.parseHost;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "run.mode", havingValue = "job")
public class SocialHandlerService {
	private final InstagramHandler instagramHandler;
	private final TikTokHandler tikTokHandler;
	private final YoutubeHandler youtubeHandler;
	private final VkHandler vkHandler;

	public void handleByHost(URI uri, Message message, String text, boolean isDeleteSourceMessage) {
		switch (parseHost(uri)) {
			case INSTAGRAM -> instagramHandler.handle(uri, message, text);
			case TIKTOK -> tikTokHandler.handle(uri, message, text);
			case YOUTUBE -> youtubeHandler.handle(uri, message, text, isDeleteSourceMessage);
			case VK -> vkHandler.handle(uri, message, text, isDeleteSourceMessage);
		}
	}
}
