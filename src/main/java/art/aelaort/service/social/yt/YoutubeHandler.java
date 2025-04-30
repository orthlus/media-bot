package art.aelaort.service.social.yt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import art.aelaort.utils.BotUtils;
import art.aelaort.exceptions.NotSendException;
import art.aelaort.exceptions.TooLargeFileException;
import art.aelaort.exceptions.YtdlpFileDownloadException;
import art.aelaort.service.social.YtdlpService;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.net.URI;
import java.nio.file.Path;

import static art.aelaort.utils.TelegramUtils.checkFileSize;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeHandler {
	private final YtdlpService ytdlp;
	private final BotUtils bot;
	@Value("${proxy-url}")
	private String proxyUrl;

	public void handle(URI uri, Update update, String text, boolean isDeleteSourceMessage) {
		try {
			checkVideoDuration(uri, update, isDeleteSourceMessage);
			Path file = ytdlp.downloadFileByUrl(uri, proxyUrl);
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
		int videoDurationSeconds = ytdlp.getVideoDurationSeconds(uri, proxyUrl);
		if (videoDurationSeconds > 30 * 60) {
			String durationText = DurationFormatUtils.formatDurationWords(videoDurationSeconds * 1000L, true, true);
			bot.sendMarkdown(update, "no no, [video](%s) duration is %s, too long".formatted(uri, durationText));
			if (isDeleteSourceMessage) {
				bot.deleteMessage(update);
			}
			return;
		}
		if (!uri.getPath().startsWith("/shorts")) {
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
