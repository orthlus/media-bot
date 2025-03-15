package main.social.tiktok;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.BotUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TikTokHandler {
	private final TikTokService tiktok;
	private final TelegramClient telegramClient;

	public void handle(URI uri, Update update, String text) {
		VideoData data = tiktok.getData(uri);
		if (tiktok.isVideo(data)) {
			tiktokSendVideo(data, uri, update, text);
		} else {
			tiktokSendImages(data, update, text);
		}
	}

	private void tiktokSendImages(VideoData data, Update update, String text) {
		List<String> imagesUrls = tiktok.getImagesUrls(data);
		BotUtils.sendImagesByUpdate(update, imagesUrls, text, telegramClient);
	}

	private void tiktokSendVideo(VideoData data, URI uri, Update update, String text) {
		List<String> urls = tiktok.getVideoMediaUrls(data);

		for (String url : urls) {
			try {
				InputStream file = tiktok.download(url);
				sendVideoByUpdate(update, text, file);

				return;
			} catch (Exception e) {
				log.error("error tiktok by {} - error send file", url, e);
			}
		}
		log.error("error send tiktok file, trying send url - {}", uri);

		for (String url : urls) {
			try {
				sendVideoByUpdate(update, text, url);

				return;
			} catch (Exception e) {
				log.error("error tiktok by {} - error send url", url, e);
			}
		}

		log.error("error send tiktok - {}", uri);
	}

	public void sendVideoByUpdate(Update update, String message, InputStream dataStream) {
		BotUtils.sendVideoByUpdate(update, message, new InputFile(dataStream, UUID.randomUUID() + ".mp4"), telegramClient);
	}

	public void sendVideoByUpdate(Update update, String message, String videoUrl) {
		BotUtils.sendVideoByUpdate(update, message, new InputFile(videoUrl), telegramClient);
	}
}
