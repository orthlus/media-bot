package main.social.yt;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import main.BotUtils;
import main.exceptions.NotSendException;
import main.exceptions.TooLargeFileException;
import main.exceptions.YoutubeFileDownloadException;
import main.social.YtdlpService;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static art.aelaort.TelegramClientHelpers.execute;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeHandler {
	private final YtdlpService youTube;
	private final TelegramClient telegramClient;

	private void checkVideoDuration(URI uri, Update update, boolean isDeleteSourceMessage) {
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

	private void checkFileSize(Path file) throws IOException {
		if (isFileTooLarge(file)) {
			Files.deleteIfExists(file);
			throw new TooLargeFileException();
		}
	}

	public void handle(URI uri, Update update, String text, boolean isDeleteSourceMessage) {
		try {
			checkVideoDuration(uri, update, isDeleteSourceMessage);
			Path file = youTube.downloadFileByUrl(uri);
			checkFileSize(file);
			BotUtils.sendVideoByUpdate(update, text, file, telegramClient);
		} catch (YoutubeFileDownloadException e) {
			log.error("youtube download error - YoutubeFileDownloadException");
			execute(SendMessage.builder()
							.chatId(update.getMessage().getChatId())
							.text("Почему то не удалось скачать [файл](%s)".formatted(uri))
							.parseMode("markdown")
							.disableWebPagePreview(true),
					telegramClient);
		} catch (TooLargeFileException e) {
			execute(SendMessage.builder()
							.chatId(update.getMessage().getChatId())
							.text("[Файл](%s) больше 2 ГБ, невозможно отправить".formatted(uri))
							.parseMode("markdown")
							.disableWebPagePreview(true),
					telegramClient);
		} catch (Exception e) {
			log.error("error send youtube url - {}", uri, e);
			throw new NotSendException();
		}
	}

	@SneakyThrows
	private boolean isFileTooLarge(Path file) {
		return Files.size(file) > 2L * 1024 * 1024 * 1024;
	}
}
