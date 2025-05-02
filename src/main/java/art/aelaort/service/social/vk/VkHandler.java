package art.aelaort.service.social.vk;

import art.aelaort.exceptions.*;
import art.aelaort.service.social.YtdlpService;
import art.aelaort.utils.BotUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.net.URI;
import java.nio.file.Path;

import static art.aelaort.utils.TelegramUtils.checkFileSize;

@Slf4j
@Component
@RequiredArgsConstructor
public class VkHandler {
	private final YtdlpService ytdlp;
	private final BotUtils bot;

	public void handle(URI uri, Message message, String text, boolean isDeleteSourceMessage) {
		try {
			checkUri(uri);
			checkVideoDuration(uri, message, isDeleteSourceMessage);
			Path file = ytdlp.downloadFileByUrl(uri);
			checkFileSize(file);
			bot.sendVideoByMessage(message, text, file);
		} catch (NotSupportedVkMediaException e) {
			bot.sendMarkdown(message, "Это ([это](%s)) не поддерживается для скачивания :(".formatted(uri));
		} catch (YtdlpFileDownloadException e) {
			log.error("vk download error - YoutubeFileDownloadException");
			bot.sendMarkdown(message, "Почему то не удалось скачать [файл](%s)".formatted(uri));
		} catch (HttpServerErrorException e) {
			log.error("vk download error - HttpServerErrorException (5xx)", e);
			bot.sendMarkdown(message, "Почему то (5xx) не удалось скачать [файл](%s)".formatted(uri));
		} catch (PotentiallyTooLargeFileException e) {
			bot.sendMarkdown(message, "[Файл](%s) видимо слишком большой, невозможно скачать".formatted(uri));
		} catch (TooLargeFileException e) {
			bot.sendMarkdown(message, "[Файл](%s) больше 2 ГБ, невозможно отправить".formatted(uri));
		} catch (AlreadyLoggedException ignored) {
		} catch (Exception e) {
			log.error("error send vk url - {}", uri, e);
			throw new NotSendException();
		}
	}

	private void checkUri(URI uri) {
		String path = uri.getPath();
		if (path.startsWith("story-") || path.contains("story")) {
			throw new NotSupportedVkMediaException();
		}
	}

	private void checkVideoDuration(URI uri, Message message, boolean isDeleteSourceMessage) {
		int videoDurationSeconds;
		try {
			videoDurationSeconds = ytdlp.getVideoDurationSeconds(uri);
		} catch (HttpServerErrorException e) {
			log.error("http error request ytdlp", e);
			bot.sendMarkdown(message, "видимо [видео](%s) недоступно, может оно 18+?".formatted(uri));
			if (isDeleteSourceMessage) {
				bot.deleteMessage(message);
			}
			throw new AlreadyLoggedException();
		}

		if (videoDurationSeconds > 30 * 60) {
			String durationText = DurationFormatUtils.formatDurationWords(videoDurationSeconds * 1000L, true, true);
			bot.sendMarkdown(message, "no no, [video](%s) duration is %s, too long".formatted(uri, durationText));
			if (isDeleteSourceMessage) {
				bot.deleteMessage(message);
			}
			throw new PotentiallyTooLargeFileException();
		}

		if (videoDurationSeconds > 120) {
			String durationText = DurationFormatUtils.formatDurationWords(videoDurationSeconds * 1000L, true, true);
			bot.sendMarkdown(message, "wow, [video](%s) duration is %s. loading...".formatted(uri, durationText));
			if (isDeleteSourceMessage) {
				bot.deleteMessage(message);
			}
		}
	}
}
