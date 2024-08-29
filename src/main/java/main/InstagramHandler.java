package main;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.exceptions.NotSendException;
import main.social.ig.InstagramService;
import main.social.ig.models.MediaUrl;
import main.social.ig.models.PhotoUrl;
import main.social.ig.models.VideoUrl;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramHandler {
	private final InstagramService instagram;
	private final TelegramClient telegramClient;

	public void handle(URI uri, Update update, String text) {
		try {
			MediaUrl url = instagram.getMediaUrl(uri);
			InputStream inputStream = instagram.download(url);
			sendMediaByUpdate(update, text, url, inputStream);
		} catch (Exception e) {
			log.error("error send instagram file, trying send url - {}", uri, e);
			try {
				MediaUrl url = instagram.getMediaUrl(uri);
				sendMediaByUpdate(update, text, url);
			} catch (RuntimeException ex) {
				log.error("error send instagram - {}", uri, e);
				throw new NotSendException();
			}
		}
	}

	public void sendMediaByUpdate(Update update, String message, MediaUrl url, InputStream... dataStream) {
		InputFile file = dataStream.length > 0 ?
				new InputFile(dataStream[0], UUID.randomUUID().toString()) :
				new InputFile(url.getUrl());

		if (url instanceof VideoUrl) {
			BotUtils.sendVideoByUpdate(update, message, file, telegramClient);
		} else if (url instanceof PhotoUrl) {
			BotUtils.sendImageByUpdate(update, message, file, telegramClient);
		}
	}
}
