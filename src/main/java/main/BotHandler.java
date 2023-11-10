package main;

import lombok.extern.slf4j.Slf4j;
import main.social.TikTokService;
import main.social.YouTubeService;
import main.social.ig.InstagramService;
import main.social.ig.KnownHosts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.starter.SpringWebhookBot;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import static main.social.ig.KnownHosts.YOUTUBE;

@Slf4j
@Component
public class BotHandler extends SpringWebhookBot {
	private final BotConfig config;
	private final String webhookUrl;

	private final InstagramService instagram;
	private final YouTubeService youTube;
	private final TikTokService tikTok;
	@Value("${bot.private_chat.id}")
	private long privateChatId;

	public BotHandler(@Value("${webhook.url}") String webhookUrl,
					  BotConfig config,
					  InstagramService instagram,
					  YouTubeService youTube,
					  TikTokService tikTok) {
		super(new SetWebhook(webhookUrl), config.getToken());
		this.config = config;
		this.webhookUrl = webhookUrl;
		this.instagram = instagram;
		this.youTube = youTube;
		this.tikTok = tikTok;
	}

	@Override
	public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
		return onWebhookUpdate(update);
	}

	@Override
	public String getBotPath() {
		return webhookUrl;
	}

	@Override
	public String getBotUsername() {
		return config.getNickname();
	}

	public BotApiMethod<?> onWebhookUpdate(Update update) {
		if (update.hasMessage() && update.getMessage().hasText()) {
			long chatId = update.getMessage().getChat().getId();
			long userId = update.getMessage().getFrom().getId();

			if (chatId == userId) {
				privateChat(update);
			} else if (chatId == privateChatId) {
				myPrivateChat(update);
			} else {
				groupChat(update);
			}
		}
		return null;
	}

	private void myPrivateChat(Update update) {
		String inputText = update.getMessage().getText();
		try {
			if (isItHost(getURL(parseUrlWithSign(inputText)), YOUTUBE)
					&& !inputText.startsWith("!"))
				return;

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
			InputStream file = tikTok.download(uri);
			sendVideoByUpdate(update, "", file);
		} catch (Exception e) {
			log.error("error handle tiktok - {}", uri, e);
			throw new NotSendException();
		}
	}

	private void youtubeUrl(URI uri, Update update) {
		try {
			InputStream file = youTube.downloadByUrl(uri);
			sendVideoByUpdate(update, "", file);
		} catch (Exception e) {
			log.error("error handling youtube url - {}", uri, e);
			throw new NotSendException();
		}
	}

	private void instagramUrl(URI uri, Update update) {
		try {
			InputStream inputStream = instagram.download(uri);
			sendVideoByUpdate(update, "", inputStream);
		} catch (Exception e) {
			log.error("error handle instagram, trying again send url - {}", uri, e);
			try {
				String url = instagram.getMediaUrl(uri);
				sendVideoByUpdate(update, "", url);
			} catch (RuntimeException ex) {
				log.error("error handle instagram - {}", uri, e);
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
				if (cleanedUrl.contains(host))
					return knownHost;
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

	public static class NotSendException extends RuntimeException{

	}
	public static class UnknownHost extends RuntimeException{
	}

	public static class InvalidUrl extends Exception {
	}
}
