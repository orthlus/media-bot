package main;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import main.social.ig.InstagramService;
import main.social.ig.KnownHosts;
import main.social.tiktok.TikTokBetaService;
import main.social.yt.YouTubeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static main.social.ig.KnownHosts.YOUTUBE;

@Slf4j
@Component
public class BotHandler extends TelegramLongPollingBot {
	@Getter
	@Value("${bot.nickname}")
	private String botUsername;

	private final InstagramService instagram;
	private final YouTubeService youTube;
	private final TikTokBetaService tikTok;
	@Value("${bot.private_chat.id}")
	private long privateChatId;
	private final Set<Long> allowedUserIds;
	private final Set<Long> allowedChatsIds;

	public BotHandler(DefaultBotOptions options,
					  @Value("${bot.token}") String token,
					  @Value("${bot.allowed.ids.users}") Long[] allowedUserIds,
					  @Value("${bot.allowed.ids.chats}") Long[] allowedChatsIds,
					  InstagramService instagram,
					  YouTubeService youTube,
					  TikTokBetaService tikTok) {
		super(options, token);
		this.instagram = instagram;
		this.youTube = youTube;
		this.tikTok = tikTok;
		this.allowedUserIds = new HashSet<>(asList(allowedUserIds));
		this.allowedChatsIds = new HashSet<>(asList(allowedChatsIds));
	}

	@Override
	public void onUpdateReceived(Update update) {
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
			if (isItHost(getURL(parseUrlWithSign(inputText)), YOUTUBE)
					&& !inputText.startsWith("!")) {
				return;
			}

			groupChat(update);
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
		} else if (inputText.equals("/tiktok_token")) {
//			tikTok.updateToken();
//			sendByUpdate("токен тикток апи обновлён", update);
			sendByUpdate("отключено", update);
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

	private void tiktokUrl(URI uri, Update update) {
		try {
			List<String> urls = tikTok.getMediaUrls(uri);

			for (String url : urls) {
				try {
					InputStream file = tikTok.download(url);
					sendVideoByUpdate(update, "", file);
					return;
				} catch (Exception e) {
					log.error("tiktok - error by {}", url);
					log.error("error send tiktok file, try another url or send directly url", e);
				}
			}
			log.error("error send tiktok file, trying send url - {}", uri);

			for (String url : urls) {
				try {
					sendVideoByUpdate(update, "", url);
					return;
				} catch (Exception e) {
					log.error("tiktok - error by {}", url);
					log.error("error send tiktok url, try another url or exit", e);
				}
			}
		} catch (Exception e) {
			log.error("error send tiktok - {}", uri, e);
			throw new NotSendException();
		}
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

	private void handleByHost(URI uri, Update update) {
		switch (parseHost(uri)) {
			case INSTAGRAM -> instagramUrl(uri, update);
			case TIKTOK -> tiktokUrl(uri, update);
			case YOUTUBE -> youtubeUrl(uri, update);
		}
	}

	private boolean isItHost(URI uri, KnownHosts host) {
		return parseHost(uri).equals(host);
	}

	private String parseUrlWithSign(String text) {
		return text.startsWith("!") ? text.substring(1) : text;
	}

	private KnownHosts parseHost(URI uri) {
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

	private void logMessageIfHasUrl(Update update) {
		try {
			getURL(update.getMessage().getText());
			long chatId = update.getMessage().getChat().getId();
			long userId = update.getMessage().getFrom().getId();
			log.info("new message {} in chat {} from {}", update.getMessage().getText(), chatId, userId);
		} catch (InvalidUrl | UnknownHost ignored) {

		}
	}

	private URI getURL(String url) throws InvalidUrl {
		try {
			return new URL(url).toURI();
		} catch (URISyntaxException | MalformedURLException e) {
			throw new InvalidUrl();
		}
	}

	private void sendByUpdate(String text, Update update) {
		Message message = update.getMessage();
		try {
			execute(SendMessage.builder()
					.chatId(message.getChatId())
					.text(text)
					.build());
		} catch (TelegramApiException e) {
			log.error("{} - Error send message '{}'", this.getClass().getName(), message.getText(), e);
		}
	}

	public void deleteMessage(Update update) {
		long chatId = update.getMessage().getChatId();
		int messageId = update.getMessage().getMessageId();
		try {
			execute(new DeleteMessage(String.valueOf(chatId), messageId));
			log.debug("{} - Deleted message, chat {} messageId {}", this.getClass().getName(), chatId, messageId);
		} catch (TelegramApiException e) {
			log.error("{} - Error delete message, chat {} messageId {}", this.getClass().getName(), chatId, messageId, e);
		}
	}

	public void sendVideoByUpdate(Update update, String message, InputStream dataStream) {
		sendVideoByUpdate(update, message, new InputFile(dataStream, UUID.randomUUID().toString()));
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
			execute(video);
		} catch (TelegramApiException e) {
			log.error("Error send message", e);
		}
	}

	public static class NotSendException extends RuntimeException {
	}

	public static class UnknownHost extends RuntimeException {
	}

	public static class InvalidUrl extends Exception {
	}
}
