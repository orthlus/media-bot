package main;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import main.exceptions.NotSendException;
import main.social.yt.YouTubeService;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static art.aelaort.TelegramClientHelpers.execute;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeHandler {
	private final YouTubeService youTube;
	private final TelegramClient telegramClient;

	private void checkVideoDuration(URI uri, Update update, TelegramClient telegramClient, boolean isDeleteSourceMessage) {
		if (!uri.getPath().startsWith("/shorts")) {
			int videoDurationSeconds = youTube.getVideoDurationSeconds(uri);
			if (videoDurationSeconds > 120) {
				String durationText = DurationFormatUtils.formatDurationWords(videoDurationSeconds * 1000L, true, true);
				execute(SendMessage.builder()
								.chatId(update.getMessage().getChatId())
								.text("wow, [video](%s) duration is %s. loading...".formatted(uri, durationText))
								.parseMode("markdown")
								.disableWebPagePreview(true),
						telegramClient);
				if (isDeleteSourceMessage) {
					execute(DeleteMessage.builder()
									.chatId(update.getMessage().getChatId())
									.messageId(update.getMessage().getMessageId()),
							telegramClient);
				}
			}
		}
	}

	public void handle(URI uri, Update update, String text, TelegramClient telegramClient, boolean isDeleteSourceMessage) {
		try {
			checkVideoDuration(uri, update, telegramClient, isDeleteSourceMessage);
			Path file = youTube.downloadFileByUrl(uri);
			if (isFileTooLarge(file)) {
				execute(SendMessage.builder()
								.chatId(update.getMessage().getChatId())
								.text("[Файл](%s) больше 2 ГБ, невозможно отправить".formatted(uri))
								.parseMode("markdown")
								.disableWebPagePreview(true),
						telegramClient);
				Files.deleteIfExists(file);
			} else {
				sendVideoByUpdate(update, text, file);
			}
		} catch (Exception e) {
			log.error("error send youtube url - {}", uri, e);
			throw new NotSendException();
		}
	}

	@SneakyThrows
	private boolean isFileTooLarge(Path file) {
		return Files.size(file) > 2L * 1024 * 1024 * 1024;
	}

	public void sendVideoByUpdate(Update update, String message, Path path) {
		BotUtils.sendVideoByUpdate(update, message, new InputFile(path.toFile(), UUID.randomUUID() + ".mp4"), telegramClient);
	}

	public void sendVideoByUpdate(Update update, String message, InputStream dataStream) {
		BotUtils.sendVideoByUpdate(update, message, new InputFile(dataStream, UUID.randomUUID() + ".mp4"), telegramClient);
	}
}
