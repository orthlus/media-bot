package art.aelaort.service;

import art.aelaort.SpringLongPollingBot;
import art.aelaort.exceptions.InvalidUrlException;
import art.aelaort.exceptions.NotSendException;
import art.aelaort.exceptions.UnknownHostException;
import art.aelaort.utils.BotUtils;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static art.aelaort.service.social.KnownHosts.YOUTUBE;
import static art.aelaort.utils.BotUtils.*;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "run.mode", havingValue = "bot")
public class BotHandler implements SpringLongPollingBot {
	private final BotUtils bot;
	private final JobService jobService;
	@Getter
	@Value("${bot.token}")
	private String botToken;

	private final TelegramClient telegramClient;

	@Value("${bot.private_chat.id}")
	private long privateChatId;
	@Value("${bot.allowed.ids.users}")
	private Long[] allowedUserIdsArr;
	@Value("${bot.allowed.ids.chats}")
	private Long[] allowedChatsIdsArr;

	private Set<Long> allowedUserIds;
	private Set<Long> allowedChatsIds;
	private final AtomicBoolean isSubstitutionEnabled = new AtomicBoolean(true);
	private final String substitutionCommandTrue = "Катя, замена";
	private final String substitutionCommandFalse = "Катя, стоп";
	private final Set<String> substitutionCommands = Set.of(substitutionCommandTrue, substitutionCommandFalse);

	@PostConstruct
	private void init() {
		this.allowedUserIds = Set.copyOf(asList(allowedUserIdsArr));
		this.allowedChatsIds = Set.copyOf(asList(allowedChatsIdsArr));
	}

	@Override
	public void consume(Update update) {
		if (!update.hasMessage() || !update.getMessage().hasText()) {
			return;
		}

		final long chatId = update.getMessage().getChatId();
		final long userId = update.getMessage().getFrom().getId();

		if (chatId == userId) {
			handlePrivateUserChat(update, userId);
		} else if (chatId == privateChatId) {
			handleMyPrivateChat(update);
		} else if (allowedChatsIds.contains(chatId)) {
			groupChat(update);
		}
	}

	private void handlePrivateUserChat(Update update, long userId) {
		if (allowedUserIds.contains(userId)) {
			privateChat(update);
		}
	}

	private void handleMyPrivateChat(Update update) {
		String inputText = update.getMessage().getText();

		if (substitutionCommands.contains(inputText)) {
			handleSubstitutionCommands(inputText, update);
			return;
		}

		try {
			URI uri = getURL(parseUrlWithSign(inputText));
			if (shouldProcessYoutube(inputText, uri)) {
				groupChat(update);
			}
		} catch (InvalidUrlException | UnknownHostException ignored) {
		}
	}

	private boolean shouldProcessYoutube(String inputText, URI uri) {
		return isItHost(uri, YOUTUBE) && (isSubstitutionEnabled.get() || inputText.startsWith("!"));
	}

	private void handleSubstitutionCommands(String inputText, Update update) {
		if (substitutionCommandTrue.equals(inputText)) {
			sendByUpdate("ок, обрабатываю ютуб", update);
			isSubstitutionEnabled.set(true);
		} else if (substitutionCommandFalse.equals(inputText)) {
			sendByUpdate("ок, больше не обрабатываю ютуб", update);
			isSubstitutionEnabled.set(false);
		}
	}

	private void groupChat(Update update) {
		String inputText = update.getMessage().getText();
		try {
			URI uri = getURL(parseUrlWithSign(inputText));
			logMessageWithUrl(update);
			String text = buildTextMessage(uri, update);
			jobService.runJob(uri, update, text, true);
			bot.deleteMessage(update);
		} catch (InvalidUrlException | UnknownHostException ignored) {
		} catch (NotSendException e) {
			log.error("Error sending message: {}", inputText, e);
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
			logMessageWithUrl(update);
			jobService.runJob(uri, update, "", false);
		} catch (InvalidUrlException e) {
			sendByUpdate("Какая-то неправильная у вас ссылка :(", update);
		} catch (UnknownHostException e) {
			sendByUpdate("Неизвестный хост", update);
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

	private void logMessageWithUrl(Update update) {
		try {
			getURL(update.getMessage().getText());
			log.info("New message in chat {} from {}: {}",
					update.getMessage().getChatId(),
					update.getMessage().getFrom().getId(),
					update.getMessage().getText());
		} catch (InvalidUrlException | UnknownHostException ignored) {
		}
	}

	private void sendByUpdate(String text, Update update) {
		BotUtils.sendByUpdate(text, update, telegramClient);
	}
}
