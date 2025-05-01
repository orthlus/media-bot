package art.aelaort.utils;

import art.aelaort.exceptions.InvalidUrlException;
import art.aelaort.exceptions.UnknownHostException;
import art.aelaort.service.social.KnownHosts;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
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

	public static void sendByMessage(String text, Message message, TelegramClient telegramClient) {
		execute(SendMessage.builder()
						.chatId(message.getChatId())
						.text(text),
				telegramClient);
	}

	public void sendMarkdown(Message message, String text) {
		execute(SendMessage.builder()
						.chatId(message.getChatId())
						.text(text)
						.parseMode("markdown")
						.disableWebPagePreview(true),
				telegramClient);
	}

	public void deleteMessage(Message message) {
		long chatId = message.getChatId();
		int messageId = message.getMessageId();
		try {
			telegramClient.execute(new DeleteMessage(String.valueOf(chatId), messageId));
			log.debug("telegram - deleted message, chat {} messageId {}", chatId, messageId);
		} catch (Exception e) {
			log.error("telegram - error delete message, chat {} messageId {}", chatId, messageId, e);
		}
	}

	public void sendVideoByMessage(Message message, String text, InputStream dataStream) {
		sendVideoByMessage(message, text, new InputFile(dataStream, UUID.randomUUID() + ".mp4"), telegramClient);
	}

	public void sendVideoByMessage(Message message, String text, Path path) {
		sendVideoByMessage(message, text, new InputFile(path.toFile(), UUID.randomUUID() + ".mp4"), telegramClient);
	}

	public static void sendVideoByMessage(Message message, String text, InputFile inputFile, TelegramClient telegramClient) {
		execute(SendVideo.builder()
						.chatId(message.getChatId())
						.caption(text)
						.parseMode("markdown")
						.video(inputFile),
				telegramClient);
	}

	public static void sendImageByMessage(Message message, String text, InputFile inputFile, TelegramClient telegramClient) {
		execute(SendPhoto.builder()
						.chatId(message.getChatId())
						.caption(text)
						.parseMode("markdown")
						.photo(inputFile),
				telegramClient);
	}

	public static void sendMediasByMessage(Message message, List<InputMedia> inputMedias, String text, TelegramClient telegramClient) {
		if (inputMedias.isEmpty()) {
			return;
		}

		if (message.isGroupMessage() || message.isSuperGroupMessage()) {
			if (inputMedias.size() > 10) {
				sendMedias(message, inputMedias.subList(0, 10), text, telegramClient);
				sleep(500);
				String s = "Больше 10 фото в группу не имею присылать((\nА прислано было %d фото".formatted(inputMedias.size());
				execute(SendMessage.builder()
								.text(s)
								.chatId(message.getChatId()),
						telegramClient);
			} else {
				sendMedias(message, inputMedias, text, telegramClient);
			}
		} else {
			List<List<InputMedia>> partitions = Lists.partition(inputMedias, 10);
			for (List<InputMedia> medias : partitions) {
				sendMedias(message, medias, text, telegramClient);
			}
		}
	}

	private static void sendMedias(Message message, List<InputMedia> photos, String text, TelegramClient telegramClient) {
		photos.get(0).setCaption(text);
		photos.get(0).setParseMode("markdown");
		execute(SendMediaGroup.builder()
						.chatId(message.getChatId())
						.medias(photos),
				telegramClient);
	}

	public static void sendImagesByMessage(Message message, List<String> imagesUrls, String text, TelegramClient telegramClient) {
		if (imagesUrls.isEmpty()) {
			return;
		}

		if (imagesUrls.size() == 1) {
			execute(SendPhoto.builder()
							.chatId(message.getChatId())
							.caption(text)
							.parseMode("markdown")
							.photo(new InputFile(imagesUrls.get(0))),
					telegramClient);
		} else {
			List<InputMediaPhoto> inputMediaPhotos = imagesUrls.stream()
					.map(InputMediaPhoto::new)
					.toList();

			if (message.isGroupMessage() || message.isSuperGroupMessage()) {
				if (inputMediaPhotos.size() > 10) {
					sendPhotos(message, inputMediaPhotos.subList(0, 10), text, telegramClient);
					sleep(500);
					String s = "Больше 10 фото в группу не имею присылать((\nА прислано было %d фото".formatted(imagesUrls.size());
					execute(SendMessage.builder()
							.text(s)
							.chatId(message.getChatId()),
							telegramClient);
				} else {
					sendPhotos(message, inputMediaPhotos, text, telegramClient);
				}
			} else {
				List<List<InputMediaPhoto>> partitions = Lists.partition(inputMediaPhotos, 10);
				for (List<InputMediaPhoto> photos : partitions) {
					sendPhotos(message, photos, text, telegramClient);
				}
			}
		}
	}

	private static void sendPhotos(Message message, List<InputMediaPhoto> photos, String text, TelegramClient telegramClient) {
		photos.get(0).setCaption(text);
		photos.get(0).setParseMode("markdown");
		execute(SendMediaGroup.builder()
						.chatId(message.getChatId())
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
