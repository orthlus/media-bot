package main;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import main.exceptions.InvalidUrl;
import main.exceptions.UnknownHost;
import main.social.ig.KnownHosts;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static art.aelaort.TelegramClientHelpers.execute;

@Slf4j
public class BotUtils {
	public static void sendByUpdate(String text, Update update, TelegramClient telegramClient) {
		Message message = update.getMessage();
		execute(SendMessage.builder()
						.chatId(message.getChatId())
						.text(text),
				telegramClient);
	}

	public static void deleteMessage(Update update, TelegramClient telegramClient) {
		long chatId = update.getMessage().getChatId();
		int messageId = update.getMessage().getMessageId();
		try {
			telegramClient.execute(new DeleteMessage(String.valueOf(chatId), messageId));
			log.debug("telegram - deleted message, chat {} messageId {}", chatId, messageId);
		} catch (Exception e) {
			log.error("telegram - error delete message, chat {} messageId {}", chatId, messageId, e);
		}
	}

	public static void sendVideoByUpdate(Update update, String message, InputFile inputFile, TelegramClient telegramClient) {
		execute(SendVideo.builder()
						.chatId(update.getMessage().getChatId())
						.caption(message)
						.parseMode("markdown")
						.video(inputFile),
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

			if (update.getMessage().isGroupMessage()) {
				List<List<InputMediaPhoto>> partitionsForLimit = Lists.partition(inputMediaPhotos, 20);
				for (List<InputMediaPhoto> partitionForLimit : partitionsForLimit) {
					List<List<InputMediaPhoto>> partitions = Lists.partition(partitionForLimit, 10);
					for (List<InputMediaPhoto> photos : partitions) {
						photos.get(0).setCaption(text);
						photos.get(0).setParseMode("markdown");
						execute(SendMediaGroup.builder()
										.chatId(update.getMessage().getChatId())
										.medias(photos),
								telegramClient);
						sleep(15);
					}
					sleep(60);
				}
			} else {
				List<List<InputMediaPhoto>> partitions = Lists.partition(inputMediaPhotos, 10);
				for (List<InputMediaPhoto> photos : partitions) {
					photos.get(0).setCaption(text);
					photos.get(0).setParseMode("markdown");
					execute(SendMediaGroup.builder()
									.chatId(update.getMessage().getChatId())
									.medias(photos),
							telegramClient);
				}
			}
		}
	}

	private static void sleep(long seconds) {
		try {
			TimeUnit.SECONDS.sleep(seconds);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static String parseUrlWithSign(String text) {
		return text.startsWith("!") ? text.substring(1) : text;
	}

	public static URI getURL(String url) throws InvalidUrl {
		try {
			return new URL(url).toURI();
		} catch (URISyntaxException | MalformedURLException e) {
			throw new InvalidUrl();
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
		throw new UnknownHost();
	}
}
