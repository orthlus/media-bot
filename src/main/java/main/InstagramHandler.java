package main;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.exceptions.NotSendException;
import main.social.ig.InstagramService;
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

	public void handle(URI uri, Update update) {
		try {
			InputStream inputStream = instagram.download(uri);
			sendVideoByUpdate(update, "", inputStream);
		} catch (Exception e) {
			log.error("error send instagram file, trying send url - {}", uri, e);
			try {
				String url = instagram.getMediaUrl(uri);
				sendVideoByUpdate(update, "", url);
			} catch (RuntimeException ex) {
				log.error("error send instagram - {}", uri, e);
				throw new NotSendException();
			}
		}
	}

	public void sendVideoByUpdate(Update update, String message, InputStream dataStream) {
		BotUtils.sendVideoByUpdate(update, message, new InputFile(dataStream, UUID.randomUUID() + ".mp4"), telegramClient);
	}

	public void sendVideoByUpdate(Update update, String message, String videoUrl) {
		BotUtils.sendVideoByUpdate(update, message, new InputFile(videoUrl), telegramClient);
	}
}
