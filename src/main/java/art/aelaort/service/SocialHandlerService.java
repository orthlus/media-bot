package art.aelaort.service;

import art.aelaort.service.social.ig.InstagramHandler;
import art.aelaort.service.social.tiktok.TikTokHandler;
import art.aelaort.service.social.vk.VkHandler;
import art.aelaort.service.social.yt.YoutubeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.net.URI;

import static art.aelaort.utils.BotUtils.parseHost;

@Component
@RequiredArgsConstructor
public class SocialHandlerService {
	private final InstagramHandler instagramHandler;
	private final TikTokHandler tikTokHandler;
	private final YoutubeHandler youtubeHandler;
	private final VkHandler vkHandler;
	private final JobService jobService;

	public void runHandlerByHost(URI uri, Update update, String text, boolean isDeleteSourceMessage) {
		jobService.runJob(uri, update, text, isDeleteSourceMessage);
	}

	public void handleByHost(URI uri, Update update, String text, boolean isDeleteSourceMessage) {
		switch (parseHost(uri)) {
			case INSTAGRAM -> instagramHandler.handle(uri, update, text);
			case TIKTOK -> tikTokHandler.handle(uri, update, text);
			case YOUTUBE -> youtubeHandler.handle(uri, update, text, isDeleteSourceMessage);
			case VK -> vkHandler.handle(uri, update, text, isDeleteSourceMessage);
		}
	}
}
