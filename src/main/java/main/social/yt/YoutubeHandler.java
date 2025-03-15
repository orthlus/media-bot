package main.social.yt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.BotUtils;
import main.exceptions.NotSendException;
import main.exceptions.TooLargeFileException;
import main.exceptions.YtdlpFileDownloadException;
import main.social.YtdlpService;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.net.URI;
import java.nio.file.Path;

import static main.TelegramUtils.checkFileSize;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeHandler {
	private final YtdlpService ytdlp;
	private final BotUtils bot;

	public void handle(URI uri, Update update, String text, boolean isDeleteSourceMessage) {
		try {
			checkVideoDuration(uri, update, isDeleteSourceMessage);
			Path file = ytdlp.downloadFileByUrl(uri);
			checkFileSize(file);
			bot.sendVideoByUpdate(update, text, file);
		} catch (YtdlpFileDownloadException e) {
			log.error("youtube download error - YoutubeFileDownloadException");
			bot.sendMarkdown(update, "Почему то не удалось скачать [файл](%s)".formatted(uri));
		} catch (TooLargeFileException e) {
			bot.sendMarkdown(update, "[Файл](%s) больше 2 ГБ, невозможно отправить".formatted(uri));
		} catch (Exception e) {
			log.error("error send youtube url - {}", uri, e);
			throw new NotSendException();
		}
	}

	private void checkVideoDuration(URI uri, Update update, boolean isDeleteSourceMessage) {
		if (!uri.getPath().startsWith("/shorts")) {
			int videoDurationSeconds = ytdlp.getVideoDurationSeconds(uri);
			if (videoDurationSeconds > 120) {
				String durationText = DurationFormatUtils.formatDurationWords(videoDurationSeconds * 1000L, true, true);
				bot.sendMarkdown(update, "wow, [video](%s) duration is %s. loading...".formatted(uri, durationText));
				if (isDeleteSourceMessage) {
					bot.deleteMessage(update);
				}
			}
		}
	}
}
