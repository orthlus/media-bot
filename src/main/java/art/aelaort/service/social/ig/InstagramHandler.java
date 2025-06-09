package art.aelaort.service.social.ig;

import art.aelaort.dto.instagram.MediaUrl;
import art.aelaort.dto.instagram.PhotoUrl;
import art.aelaort.dto.instagram.VideoUrl;
import art.aelaort.exceptions.DownloadMediaUnknownException;
import art.aelaort.exceptions.NotSendException;
import art.aelaort.utils.BotUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.api.objects.message.Message;
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
	private final BotUtils botUtils;

	public void handle(URI uri, Message message, String text) {
		try {
			sendBytes(uri, message, text);
		} catch (Exception e) {
			log.error("error send instagram file, trying send url - {}", uri, e);
			try {
				sendLinks(uri, message, text);
			} catch (DownloadMediaUnknownException ex) {
				log.error("error downloading media", e);
				botUtils.sendMarkdown(message, "Не удалось обработать [ссылку](%s) :(".formatted(uri));
			} catch (RuntimeException ex) {
				log.error("error send instagram - {}", uri, e);
				throw new NotSendException();
			}
		}
	}

	private void sendLinks(URI uri, Message message, String text) {
		List<MediaUrl> urls = instagram.getMediaUrls(uri);
		if (urls.isEmpty()) {
			throw new DownloadMediaUnknownException();
		}
		if (urls.size() == 1) {
			sendMediaByMessage(message, text, urls.get(0), new InputFile(urls.get(0).getUrl()));
		} else {
			List<InputMedia> list = urls.stream().map(this::getInputMediaLink).toList();
			BotUtils.sendMediasByMessage(message, list, text, telegramClient);
		}
	}

	private void sendBytes(URI uri, Message message, String text) {
		List<MediaUrl> urls = instagram.getMediaUrls(uri);
		if (urls.isEmpty()) {
			throw new DownloadMediaUnknownException();
		}
		if (urls.size() == 1) {
			InputStream inputStream = instagram.download(urls.get(0));
			sendMediaByMessage(message, text, urls.get(0), getISByUrl(inputStream));
		} else {
			List<InputMedia> list = urls.stream().map(this::getInputMediaIS).toList();
			BotUtils.sendMediasByMessage(message, list, text, telegramClient);
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

	private void sendMediaByMessage(Message message, String text, MediaUrl url, InputFile file) {
		if (url instanceof VideoUrl) {
			BotUtils.sendVideoByMessage(message, text, file, telegramClient);
		} else if (url instanceof PhotoUrl) {
			BotUtils.sendImageByMessage(message, text, file, telegramClient);
		}
	}
}
