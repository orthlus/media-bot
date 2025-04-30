package art.aelaort.service.social.ig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import art.aelaort.utils.BotUtils;
import art.aelaort.exceptions.NotSendException;
import art.aelaort.service.social.ig.models.MediaUrl;
import art.aelaort.service.social.ig.models.PhotoUrl;
import art.aelaort.service.social.ig.models.VideoUrl;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramHandler {
	private final InstagramService instagram;
	private final TelegramClient telegramClient;

	public void handle(URI uri, Update update, String text) {
		try {
			sendBytes(uri, update, text);
		} catch (Exception e) {
			log.error("error send instagram file, trying send url - {}", uri, e);
			try {
				sendLinks(uri, update, text);
			} catch (RuntimeException ex) {
				log.error("error send instagram - {}", uri, e);
				throw new NotSendException();
			}
		}
	}

	private void sendLinks(URI uri, Update update, String text) {
		List<MediaUrl> urls = instagram.getMediaUrls(uri);
		if (urls.size() == 1) {
			sendMediaByUpdate(update, text, urls.get(0), new InputFile(urls.get(0).getUrl()));
		} else {
			List<InputMedia> list = urls.stream().map(this::getInputMediaLink).toList();
			BotUtils.sendMediasByUpdate(update, list, text, telegramClient);
		}
	}

	private void sendBytes(URI uri, Update update, String text) {
		List<MediaUrl> urls = instagram.getMediaUrls(uri);
		if (urls.size() == 1) {
			InputStream inputStream = instagram.download(urls.get(0));
			sendMediaByUpdate(update, text, urls.get(0), getISByUrl(inputStream));
		} else {
			List<InputMedia> list = urls.stream().map(this::getInputMediaIS).toList();
			BotUtils.sendMediasByUpdate(update, list, text, telegramClient);
		}
	}

	private InputFile getISByUrl(InputStream inputStream) {
		return new InputFile(inputStream, UUID.randomUUID().toString());
	}

	private InputMedia getInputMediaIS(MediaUrl url) {
		if (url instanceof VideoUrl) {
			return new InputMediaVideo(instagram.download(url), UUID.randomUUID().toString());
		} else if (url instanceof PhotoUrl) {
			return new InputMediaPhoto(instagram.download(url), UUID.randomUUID().toString());
		}

		throw new IllegalArgumentException();
	}

	private InputMedia getInputMediaLink(MediaUrl url) {
		if (url instanceof VideoUrl) {
			return new InputMediaVideo(url.getUrl());
		} else if (url instanceof PhotoUrl) {
			return new InputMediaPhoto(url.getUrl());
		}

		throw new IllegalArgumentException();
	}

	private void sendMediaByUpdate(Update update, String message, MediaUrl url, InputFile file) {
		if (url instanceof VideoUrl) {
			BotUtils.sendVideoByUpdate(update, message, file, telegramClient);
		} else if (url instanceof PhotoUrl) {
			BotUtils.sendImageByUpdate(update, message, file, telegramClient);
		}
	}
}
