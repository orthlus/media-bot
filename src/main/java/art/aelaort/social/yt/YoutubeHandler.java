package art.aelaort.social.yt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import art.aelaort.BotUtils;
import art.aelaort.exceptions.NotSendException;
import art.aelaort.exceptions.TooLargeFileException;
import art.aelaort.exceptions.YtdlpFileDownloadException;
import art.aelaort.social.YtdlpService;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.net.URI;
import java.nio.file.Path;

import static art.aelaort.TelegramUtils.checkFileSize;

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
		} catch (HttpServerErrorException e) {
			log.error("youtube download error - HttpServerErrorException (5xx)");
			bot.sendMarkdown(update, "Почему то (5xx) не удалось скачать [файл](%s)".formatted(uri));
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
