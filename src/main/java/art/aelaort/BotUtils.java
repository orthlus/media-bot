package art.aelaort;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import art.aelaort.exceptions.InvalidUrlException;
import art.aelaort.exceptions.UnknownHostException;
import art.aelaort.social.KnownHosts;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static art.aelaort.TelegramClientHelpers.execute;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotUtils {
	private final TelegramClient telegramClient;

	public static void sendByUpdate(String text, Update update, TelegramClient telegramClient) {
		Message message = update.getMessage();
		execute(SendMessage.builder()
						.chatId(message.getChatId())
						.text(text),
				telegramClient);
	}

	public void sendMarkdown(Update update, String text) {
		execute(SendMessage.builder()
						.chatId(update.getMessage().getChatId())
						.text(text)
						.parseMode("markdown")
						.disableWebPagePreview(true),
				telegramClient);
	}

	public void deleteMessage(Update update) {
		long chatId = update.getMessage().getChatId();
		int messageId = update.getMessage().getMessageId();
		try {
			telegramClient.execute(new DeleteMessage(String.valueOf(chatId), messageId));
			log.debug("telegram - deleted message, chat {} messageId {}", chatId, messageId);
		} catch (Exception e) {
			log.error("telegram - error delete message, chat {} messageId {}", chatId, messageId, e);
		}
	}

	public void sendVideoByUpdate(Update update, String message, InputStream dataStream) {
		sendVideoByUpdate(update, message, new InputFile(dataStream, UUID.randomUUID() + ".mp4"), telegramClient);
	}

	public void sendVideoByUpdate(Update update, String message, Path path) {
		sendVideoByUpdate(update, message, new InputFile(path.toFile(), UUID.randomUUID() + ".mp4"), telegramClient);
	}

	public static void sendVideoByUpdate(Update update, String message, InputFile inputFile, TelegramClient telegramClient) {
		execute(SendVideo.builder()
						.chatId(update.getMessage().getChatId())
						.caption(message)
						.parseMode("markdown")
						.video(inputFile),
				telegramClient);
	}

	public static void sendImageByUpdate(Update update, String message, InputFile inputFile, TelegramClient telegramClient) {
		execute(SendPhoto.builder()
						.chatId(update.getMessage().getChatId())
						.caption(message)
						.parseMode("markdown")
						.photo(inputFile),
				telegramClient);
	}

	public static void sendMediasByUpdate(Update update, List<InputMedia> inputMedias, String text, TelegramClient telegramClient) {
		if (inputMedias.isEmpty()) {
			return;
		}

		if (update.getMessage().isGroupMessage() || update.getMessage().isSuperGroupMessage()) {
			if (inputMedias.size() > 10) {
				sendMedias(update, inputMedias.subList(0, 10), text, telegramClient);
				sleep(500);
				String s = "Больше 10 фото в группу не имею присылать((\nА прислано было %d фото".formatted(inputMedias.size());
				execute(SendMessage.builder()
								.text(s)
								.chatId(update.getMessage().getChatId()),
						telegramClient);
			} else {
				sendMedias(update, inputMedias, text, telegramClient);
			}
		} else {
			List<List<InputMedia>> partitions = Lists.partition(inputMedias, 10);
			for (List<InputMedia> medias : partitions) {
				sendMedias(update, medias, text, telegramClient);
			}
		}
	}

	private static void sendMedias(Update update, List<InputMedia> photos, String text, TelegramClient telegramClient) {
		photos.get(0).setCaption(text);
		photos.get(0).setParseMode("markdown");
		execute(SendMediaGroup.builder()
						.chatId(update.getMessage().getChatId())
						.medias(photos),
				telegramClient);
	}

	public static void sendImagesByUpdate(Update update, List<String> imagesUrls, String text, TelegramClient telegramClient) {
		if (imagesUrls.isEmpty()) {
			return;
		}

		if (imagesUrls.size() == 1) {
			execute(SendPhoto.builder()
							.chatId(update.getMessage().getChatId())
							.caption(text)
							.parseMode("markdown")
							.photo(new InputFile(imagesUrls.get(0))),
					telegramClient);
		} else {
			List<InputMediaPhoto> inputMediaPhotos = imagesUrls.stream()
					.map(InputMediaPhoto::new)
					.toList();

			if (update.getMessage().isGroupMessage() || update.getMessage().isSuperGroupMessage()) {
				if (inputMediaPhotos.size() > 10) {
					sendPhotos(update, inputMediaPhotos.subList(0, 10), text, telegramClient);
					sleep(500);
					String s = "Больше 10 фото в группу не имею присылать((\nА прислано было %d фото".formatted(imagesUrls.size());
					execute(SendMessage.builder()
							.text(s)
							.chatId(update.getMessage().getChatId()),
							telegramClient);
				} else {
					sendPhotos(update, inputMediaPhotos, text, telegramClient);
				}
			} else {
				List<List<InputMediaPhoto>> partitions = Lists.partition(inputMediaPhotos, 10);
				for (List<InputMediaPhoto> photos : partitions) {
					sendPhotos(update, photos, text, telegramClient);
				}
			}
		}
	}

	private static void sendPhotos(Update update, List<InputMediaPhoto> photos, String text, TelegramClient telegramClient) {
		photos.get(0).setCaption(text);
		photos.get(0).setParseMode("markdown");
		execute(SendMediaGroup.builder()
						.chatId(update.getMessage().getChatId())
						.medias(photos),
				telegramClient);
	}

	private static void sleep(long milliseconds) {
		try {
			TimeUnit.MILLISECONDS.sleep(milliseconds);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static String parseUrlWithSign(String text) {
		return text.startsWith("!") ? text.substring(1) : text;
	}

	public static URI getURL(String url) throws InvalidUrlException {
		try {
			return new URL(url).toURI();
		} catch (URISyntaxException | MalformedURLException e) {
			throw new InvalidUrlException();
		}
	}

	public static boolean isItHost(URI uri, KnownHosts host) {
		return parseHost(uri).equals(host);
	}

	public static KnownHosts parseHost(URI uri) {
		String cleanedUrl = uri.getHost().replace("www.", "");
		for (KnownHosts knownHost : KnownHosts.values()) {
			for (String host : knownHost.getHosts()) {
				if (cleanedUrl.contains(host)) {
					return knownHost;
				}
			}
		}
		throw new UnknownHostException();
	}
}
