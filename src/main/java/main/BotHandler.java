package main;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import main.exceptions.InvalidUrl;
import main.exceptions.NotSendException;
import main.exceptions.UnknownHost;
import main.social.ig.InstagramService;
import main.social.tiktok.TikTok;
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

import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;
import static main.BotUtils.getURL;
import static main.BotUtils.parseUrlWithSign;
import static main.BotUtils.isItHost;
import static main.BotUtils.parseHost;
import static main.social.ig.KnownHosts.YOUTUBE;

@Slf4j
@Component
public class BotHandler extends TelegramLongPollingBot {
	@Getter
	@Value("${bot.nickname}")
	private String botUsername;

	private final InstagramService instagram;
	private final YouTubeService youTube;
	private final List<TikTok> tiktokServices;
	@Value("${bot.private_chat.id}")
	private long privateChatId;
	private final Set<Long> allowedUserIds;
	private final Set<Long> allowedChatsIds;
	private final AtomicBoolean substitution = new AtomicBoolean(true);

	public BotHandler(DefaultBotOptions options,
					  @Value("${bot.token}") String token,
					  @Value("${bot.allowed.ids.users}") Long[] allowedUserIds,
					  @Value("${bot.allowed.ids.chats}") Long[] allowedChatsIds,
					  InstagramService instagram,
					  YouTubeService youTube,
					  List<TikTok> tiktokServices) {
		super(options, token);
		this.instagram = instagram;
		this.youTube = youTube;
		this.allowedUserIds = new HashSet<>(asList(allowedUserIds));
		this.allowedChatsIds = new HashSet<>(asList(allowedChatsIds));
		this.tiktokServices = tiktokServices;
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
		for (TikTok tiktokService : tiktokServices) {
			try {
				boolean success = tiktokHandleByService(tiktokService, uri, update);

				if (success) {
					return;
				}
			} catch (Exception e) {
				log.error("error send tiktok - {} by service {}", uri, tiktokService.getTiktokServiceName(), e);
			}
		}

		throw new NotSendException();
	}

	private boolean tiktokHandleByService(TikTok tikTok, URI uri, Update update) {
		List<String> urls = tikTok.getMediaUrls(uri);

		for (String url : urls) {
			try {
				InputStream file = tikTok.download(url);
				sendVideoByUpdate(update, "", file);

				return true;
			} catch (Exception e) {
				log.error("error tiktok by {} - error send file", url, e);
			}
		}
		log.error("error send tiktok file, trying send url - {}", uri);

		for (String url : urls) {
			try {
				sendVideoByUpdate(update, "", url);

				return true;
			} catch (Exception e) {
				log.error("error tiktok by {} - error send url", url, e);
			}
		}

		log.error("error send tiktok - {} by service {}", uri, tikTok.getTiktokServiceName());
		return false;
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
			execute(SendMessage.builder()
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
			execute(new DeleteMessage(String.valueOf(chatId), messageId));
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
			execute(video);
		} catch (Exception e) {
			log.error("Error send message", e);
			throw new RuntimeException(e);
		}
	}

	public static class NotSendException extends RuntimeException {
	}

	}
}
