package main;

import art.aelaort.SpringLongPollingBot;
import com.google.common.collect.Lists;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.exceptions.InvalidUrl;
import main.exceptions.NotSendException;
import main.exceptions.UnknownHost;
import main.social.ig.InstagramService;
import main.social.tiktok.TikTokService;
import main.social.tiktok.VideoData;
import main.social.yt.YouTubeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
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

import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;
import static main.BotUtils.*;
import static main.social.ig.KnownHosts.YOUTUBE;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotHandler implements SpringLongPollingBot {
	@Getter
	@Value("${bot.token}")
	private String botToken;

	private final InstagramService instagram;
	private final YouTubeService youTube;
	private final TikTokService tiktok;
	private final TelegramClient telegramClient;

	@Value("${bot.private_chat.id}")
	private long privateChatId;
	@Value("${bot.allowed.ids.users}")
	private Long[] allowedUserIdsArr;
	@Value("${bot.allowed.ids.chats}")
	private Long[] allowedChatsIdsArr;

	private Set<Long> allowedUserIds;
	private Set<Long> allowedChatsIds;
	private final AtomicBoolean substitution = new AtomicBoolean(true);

	@PostConstruct
	private void init() {
		this.allowedUserIds = new HashSet<>(asList(allowedUserIdsArr));
		this.allowedChatsIds = new HashSet<>(asList(allowedChatsIdsArr));
	}

	@Override
	public void consume(Update update) {
		if (update.hasMessage() && update.getMessage().hasText()) {
			long chatId = update.getMessage().getChat().getId();
			long userId = update.getMessage().getFrom().getId();

			if (chatId == userId) {
				if (allowedUserIds.contains(userId)) {
					privateChat(update);
				}
			} else if (chatId == privateChatId) {
				myPrivateChat(update);
			} else {
				if (allowedChatsIds.contains(chatId)) {
					groupChat(update);
				}
			}
		}
	}

	private void myPrivateChat(Update update) {
		String inputText = update.getMessage().getText();
		try {
			if (inputText.equals("Катя, замена")) {
				sendByUpdate("ок, обрабатываю ютуб", update);
				substitution.set(true);
				return;
			} else if (inputText.equals("Катя, стоп")) {
				sendByUpdate("ок, больше не обрабатываю ютуб", update);
				substitution.set(false);
				return;
			}

			if (isItHost(getURL(parseUrlWithSign(inputText)), YOUTUBE)) {
				if (substitution.get()) {
					groupChat(update);
				} else if (inputText.startsWith("!")) {
					groupChat(update);
				}
			} else {
				groupChat(update);
			}
		} catch (InvalidUrl | UnknownHost ignored) {
		}
	}

	private void groupChat(Update update) {
		String inputText = update.getMessage().getText();
		try {
			URI uri = getURL(parseUrlWithSign(inputText));
			logMessageIfHasUrl(update);
			handleByHost(uri, update);
			deleteMessage(update);
		} catch (InvalidUrl | UnknownHost ignored) {
		} catch (NotSendException e) {
			log.error("error sending message by {}", inputText);
		}
	}

	private void privateChat(Update update) {
		String inputText = update.getMessage().getText();
		if (inputText.equals("/start")) {
			sendByUpdate("Привет! Скачаю медиа по ссылке", update);
			return;
		}
		try {
			URI uri = getURL(inputText);
			logMessageIfHasUrl(update);
			handleByHost(uri, update);
		} catch (InvalidUrl e) {
			sendByUpdate("Какая-то неправильная у вас ссылка :(", update);
		} catch (UnknownHost e) {
			sendByUpdate("Неизвестный хост", update);
		}
	}

	private void handleByHost(URI uri, Update update) {
		switch (parseHost(uri)) {
			case INSTAGRAM -> instagramUrl(uri, update);
			case TIKTOK -> tiktokUrl(uri, update);
			case YOUTUBE -> youtubeUrl(uri, update);
		}
	}

	private void tiktokUrl(URI uri, Update update) {
		VideoData data = tiktok.getData(uri);
		if (tiktok.isVideo(data)) {
			tiktokSendVideo(data, uri, update);
		} else {
			tiktokSendImages(data, update);
		}
	}

	private void tiktokSendImages(VideoData data, Update update) {
		List<String> imagesUrls = tiktok.getImagesUrls(data);
		sendImagesByUpdate(update, imagesUrls);
	}

	private void tiktokSendVideo(VideoData data, URI uri, Update update) {
		List<String> urls = tiktok.getVideoMediaUrls(data);

		for (String url : urls) {
			try {
				InputStream file = tiktok.download(url);
				sendVideoByUpdate(update, "", file);

				return;
			} catch (Exception e) {
				log.error("error tiktok by {} - error send file", url, e);
			}
		}
		log.error("error send tiktok file, trying send url - {}", uri);

		for (String url : urls) {
			try {
				sendVideoByUpdate(update, "", url);

				return;
			} catch (Exception e) {
				log.error("error tiktok by {} - error send url", url, e);
			}
		}

		log.error("error send tiktok - {}", uri);
	}

	private void youtubeUrl(URI uri, Update update) {
		try {
			InputStream file = youTube.downloadByUrl(uri);
			sendVideoByUpdate(update, "", file);
		} catch (Exception e) {
			log.error("error send youtube url - {}", uri, e);
			throw new NotSendException();
		}
	}

	private void instagramUrl(URI uri, Update update) {
		try {
			InputStream inputStream = instagram.download(uri);
			sendVideoByUpdate(update, "", inputStream);
		} catch (Exception e) {
			log.error("error send instagram file, trying send url - {}", uri, e);
			try {
				String url = instagram.getMediaUrl(uri);
				sendVideoByUpdate(update, "", url);
			} catch (RuntimeException ex) {
				log.error("error send instagram - {}", uri, e);
				throw new NotSendException();
			}
		}
	}

	private void logMessageIfHasUrl(Update update) {
		try {
			getURL(update.getMessage().getText());
			long chatId = update.getMessage().getChat().getId();
			long userId = update.getMessage().getFrom().getId();
			log.info("new message {} in chat {} from {}", update.getMessage().getText(), chatId, userId);
		} catch (InvalidUrl | UnknownHost ignored) {

		}
	}

	private void sendByUpdate(String text, Update update) {
		Message message = update.getMessage();
		try {
			telegramClient.execute(SendMessage.builder()
					.chatId(message.getChatId())
					.text(text)
					.build());
		} catch (Exception e) {
			log.error("{} - Error send message '{}'", this.getClass().getName(), message.getText(), e);
		}
	}

	public void deleteMessage(Update update) {
		long chatId = update.getMessage().getChatId();
		int messageId = update.getMessage().getMessageId();
		try {
			telegramClient.execute(new DeleteMessage(String.valueOf(chatId), messageId));
			log.debug("{} - Deleted message, chat {} messageId {}", this.getClass().getName(), chatId, messageId);
		} catch (Exception e) {
			log.error("{} - Error delete message, chat {} messageId {}", this.getClass().getName(), chatId, messageId, e);
		}
	}

	public void sendVideoByUpdate(Update update, String message, InputStream dataStream) {
		sendVideoByUpdate(update, message, new InputFile(dataStream, UUID.randomUUID() + ".mp4"));
	}

	public void sendVideoByUpdate(Update update, String message, String videoUrl) {
		sendVideoByUpdate(update, message, new InputFile(videoUrl));
	}

	public void sendVideoByUpdate(Update update, String message, InputFile inputFile) {
		try {
			SendVideo video = SendVideo.builder()
					.chatId(update.getMessage().getChatId())
					.caption(message)
					.video(inputFile)
					.build();
			telegramClient.execute(video);
		} catch (Exception e) {
			log.error("Error send message", e);
			throw new RuntimeException(e);
		}
	}

	public void sendImagesByUpdate(Update update, List<String> imagesUrls) {
		try {
			if (imagesUrls.isEmpty()) {
				return;
			}

			if (imagesUrls.size() == 1) {
				SendPhoto photo = SendPhoto.builder()
						.chatId(update.getMessage().getChatId())
						.photo(new InputFile(imagesUrls.get(0)))
						.build();
				telegramClient.execute(photo);
			} else {
				List<InputMediaPhoto> inputMediaPhotos = imagesUrls.stream()
						.map(InputMediaPhoto::new)
						.toList();
				List<List<InputMediaPhoto>> partitions = Lists.partition(inputMediaPhotos, 10);
				for (List<InputMediaPhoto> photos : partitions) {
					SendMediaGroup media = SendMediaGroup.builder()
							.chatId(update.getMessage().getChatId())
							.medias(photos)
							.build();
					telegramClient.execute(media);
				}
			}
		} catch (Exception e) {
			log.error("Error send message", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public LongPollingUpdateConsumer getUpdatesConsumer() {
		return this;
	}
}
