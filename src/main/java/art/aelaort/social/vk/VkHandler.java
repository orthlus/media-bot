package art.aelaort.social.vk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import art.aelaort.BotUtils;
import art.aelaort.exceptions.NotSendException;
import art.aelaort.exceptions.NotSupportedVkMediaException;
import art.aelaort.exceptions.TooLargeFileException;
import art.aelaort.exceptions.YtdlpFileDownloadException;
import art.aelaort.social.YtdlpService;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.net.URI;
import java.nio.file.Path;

import static art.aelaort.TelegramUtils.checkFileSize;

@Slf4j
@Component
@RequiredArgsConstructor
public class VkHandler {
	private final YtdlpService ytdlp;
	private final BotUtils bot;
	@Value("${vk.proxy-url}")
	private String proxyUrl;

	public void handle(URI uri, Update update, String text, boolean isDeleteSourceMessage) {
		try {
			checkUri(uri);
			checkVideoDuration(uri, update, isDeleteSourceMessage);
			Path file = ytdlp.downloadFileByUrl(uri, proxyUrl);
			checkFileSize(file);
			bot.sendVideoByUpdate(update, text, file);
		} catch (NotSupportedVkMediaException e) {
			bot.sendMarkdown(update, "Это ([это](%s)) не поддерживается для скачивания :(".formatted(uri));
		} catch (YtdlpFileDownloadException e) {
			log.error("vk download error - YoutubeFileDownloadException");
			bot.sendMarkdown(update, "Почему то не удалось скачать [файл](%s)".formatted(uri));
		} catch (HttpServerErrorException e) {
			log.error("vk download error - HttpServerErrorException (5xx)");
			bot.sendMarkdown(update, "Почему то не удалось скачать [файл](%s)".formatted(uri));
		} catch (TooLargeFileException e) {
			bot.sendMarkdown(update, "[Файл](%s) больше 2 ГБ, невозможно отправить".formatted(uri));
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

	private void checkVideoDuration(URI uri, Update update, boolean isDeleteSourceMessage) {
		int videoDurationSeconds = ytdlp.getVideoDurationSeconds(uri, proxyUrl);
		if (videoDurationSeconds > 120) {
			String durationText = DurationFormatUtils.formatDurationWords(videoDurationSeconds * 1000L, true, true);
			bot.sendMarkdown(update, "wow, [video](%s) duration is %s. loading...".formatted(uri, durationText));
			if (isDeleteSourceMessage) {
				bot.deleteMessage(update);
			}
		}
	}
}
