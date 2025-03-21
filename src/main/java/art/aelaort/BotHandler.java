package art.aelaort;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import art.aelaort.exceptions.InvalidUrlException;
import art.aelaort.exceptions.NotSendException;
import art.aelaort.exceptions.UnknownHostException;
import art.aelaort.social.ig.InstagramHandler;
import art.aelaort.social.tiktok.TikTokHandler;
import art.aelaort.social.vk.VkHandler;
import art.aelaort.social.yt.YoutubeHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;
import static art.aelaort.BotUtils.*;
import static art.aelaort.social.KnownHosts.YOUTUBE;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotHandler implements SpringLongPollingBot {
	private final BotUtils bot;
	private final VkHandler vkHandler;
	@Getter
	@Value("${bot.token}")
	private String botToken;

	private final InstagramHandler instagramHandler;
	private final YoutubeHandler youtubeHandler;
	private final TikTokHandler tikTokHandler;
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
		} catch (InvalidUrlException | UnknownHostException ignored) {
		}
	}

	private void groupChat(Update update) {
		String inputText = update.getMessage().getText();
		try {
			URI uri = getURL(parseUrlWithSign(inputText));
			logMessageIfHasUrl(update);
			String text = buildTextMessage(uri, update);
			handleByHost(uri, update, text, true);
			bot.deleteMessage(update);
		} catch (InvalidUrlException | UnknownHostException ignored) {
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
			handleByHost(uri, update, "", false);
		} catch (InvalidUrlException e) {
			sendByUpdate("Какая-то неправильная у вас ссылка :(", update);
		} catch (UnknownHostException e) {
			sendByUpdate("Неизвестный хост", update);
		}
	}

	private void handleByHost(URI uri, Update update, String text, boolean isDeleteSourceMessage) {
		switch (parseHost(uri)) {
			case INSTAGRAM -> instagramHandler.handle(uri, update, text);
			case TIKTOK -> tikTokHandler.handle(uri, update, text);
			case YOUTUBE -> youtubeHandler.handle(uri, update, text, isDeleteSourceMessage);
			case VK -> vkHandler.handle(uri, update, text, isDeleteSourceMessage);
		}
	}

	private String buildTextMessage(URI uri, Update update) {
		try {
			User user = update.getMessage().getFrom();
			String lastName = user.getLastName();
			String firstName = user.getFirstName();
			String userName = user.getUserName();

			String name;
			if (isNotEmpty(userName)) {
				name = "t.me/" + userName;
			} else {
				if (isEmpty(lastName)) {
					name = firstName;
				} else {
					name = firstName + " " + lastName;
				}
			}
			String serviceName = parseHost(uri).getText();

			return "[%s](%s) прислал это из [%s](%s)".formatted(userName, name, serviceName, uri.toString());
		} catch (Exception e) {
			log.error("error in buildTextMessage", e);
			return "";
		}
	}

	private void logMessageIfHasUrl(Update update) {
		try {
			getURL(update.getMessage().getText());
			long chatId = update.getMessage().getChat().getId();
			long userId = update.getMessage().getFrom().getId();
			log.info("new message {} in chat {} from {}", update.getMessage().getText(), chatId, userId);
		} catch (InvalidUrlException | UnknownHostException ignored) {

		}
	}

	private void sendByUpdate(String text, Update update) {
		BotUtils.sendByUpdate(text, update, telegramClient);
	}
}
