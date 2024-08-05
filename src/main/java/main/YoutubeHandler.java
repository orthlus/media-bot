package main;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.exceptions.NotSendException;
import main.social.yt.YouTubeService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeHandler {
	private final YouTubeService youTube;
	private final TelegramClient telegramClient;

	public void handle(URI uri, Update update) {
		handle(uri, update, "");
	}

	public void handle(URI uri, Update update, String text) {
		try {
			Path file = youTube.downloadFileByUrl(uri);
			sendVideoByUpdate(update, text, file);
		} catch (Exception e) {
			log.error("error send youtube url - {}", uri, e);
			throw new NotSendException();
		}
	}

	public void sendVideoByUpdate(Update update, String message, Path path) {
		BotUtils.sendVideoByUpdate(update, message, new InputFile(path.toFile(), UUID.randomUUID() + ".mp4"), telegramClient);
	}

	public void sendVideoByUpdate(Update update, String message, InputStream dataStream) {
		BotUtils.sendVideoByUpdate(update, message, new InputFile(dataStream, UUID.randomUUID() + ".mp4"), telegramClient);
	}
}
