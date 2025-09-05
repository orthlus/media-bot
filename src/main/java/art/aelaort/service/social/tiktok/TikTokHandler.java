package art.aelaort.service.social.tiktok;

import art.aelaort.dto.tiktok.VideoData;
import art.aelaort.exceptions.DownloadMediaUnknownException;
import art.aelaort.utils.BotUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TikTokHandler {
	private final TikTokService tiktok;
	private final TelegramClient telegramClient;
	private final BotUtils botUtils;

	public void handle(URI uri, Message message, String text) {
		try {
			VideoData data = tiktok.getData(uri);
			if (tiktok.isVideo(data)) {
				tiktokSendVideo(data, uri, message, text);
			} else {
				tiktokSendImages(data, message, text);
			}
		} catch (DownloadMediaUnknownException e) {
			log.error("error downloading media", e);
			botUtils.sendMarkdown(message, "Не удалось обработать [ссылку](%s) :(".formatted(uri));
		}
	}

	private void tiktokSendImages(VideoData data, Message message, String text) {
		List<String> imagesUrls = tiktok.getImagesUrls(data);
		if (imagesUrls.isEmpty()) {
			throw new DownloadMediaUnknownException();
		}

		if (imagesUrls.size() == 1) {
			String url = imagesUrls.get(0);
			try {
				sendImageByMessage(message, text, url);
			} catch (Exception e) {
				log.error("error sending tiktok image by url, try downloading... Error: {}", e.getMessage());
				sendImageByMessage(message, text, tiktok.download(url));
			}
		} else {
			try {
				List<InputMediaPhoto> urls = imagesUrls.stream().map(InputMediaPhoto::new).toList();
				BotUtils.sendMediasByMessage(message, urls, text, telegramClient);
			} catch (Exception e) {
				log.error("error sending tiktok images by url, try downloading... Error: {}", e.getMessage());
				List<InputMediaPhoto> urls = imagesUrls.stream()
						.map(url -> new InputMediaPhoto(tiktok.download(url), UUID.randomUUID().toString()))
						.toList();
				BotUtils.sendMediasByMessage(message, urls, text, telegramClient);
			}
		}
	}

	private void tiktokSendVideo(VideoData data, URI uri, Message message, String text) {
		List<String> urls = tiktok.getVideoMediaUrls(data);

		for (String url : urls) {
			try {
//				InputStream file = tiktok.download(url);
//				sendVideoByMessage(message, text, file);
				Path file = tiktok.downloadFile(url);
				sendVideoByMessage(message, text, file);

				return;
			} catch (Exception e) {
				log.error("error tiktok by {} - error send file", url, e);
			}
		}
		log.error("error send tiktok file, trying send url - {}", uri);

		for (String url : urls) {
			try {
				sendVideoByMessage(message, text, url);

				return;
			} catch (Exception e) {
				log.error("error tiktok by {} - error send url", url, e);
			}
		}

		log.error("error send tiktok - {}", uri);
	}

	public void sendVideoByMessage(Message message, String text, Path file) {
		BotUtils.sendVideoByMessage(message, text, new InputFile(file.toFile(), UUID.randomUUID() + ".mp4"), telegramClient);
	}

	public void sendVideoByMessage(Message message, String text, InputStream dataStream) {
		BotUtils.sendVideoByMessage(message, text, new InputFile(dataStream, UUID.randomUUID() + ".mp4"), telegramClient);
	}

	public void sendVideoByMessage(Message message, String text, String videoUrl) {
		BotUtils.sendVideoByMessage(message, text, new InputFile(videoUrl), telegramClient);
	}

	public void sendImageByMessage(Message message, String text, InputStream dataStream) {
		BotUtils.sendImageByMessage(message, text, new InputFile(dataStream, UUID.randomUUID().toString()), telegramClient);
	}

	public void sendImageByMessage(Message message, String text, String imageUrl) {
		BotUtils.sendImageByMessage(message, text, new InputFile(imageUrl), telegramClient);
	}
}
