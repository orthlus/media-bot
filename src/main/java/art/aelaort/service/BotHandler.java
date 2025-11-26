package art.aelaort.service;

import art.aelaort.telegram.SimpleLongPollingBot;
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
import org.telegram.telegrambots.meta.api.objects.message.Message;
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
public class BotHandler implements SimpleLongPollingBot {
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

		Message message = update.getMessage();
		final long chatId = message.getChatId();
		final long userId = message.getFrom().getId();

		if (chatId == userId) {
			handlePrivateUserChat(message, userId);
		} else if (chatId == privateChatId) {
			handleMyPrivateChat(message);
		} else if (allowedChatsIds.contains(chatId)) {
			groupChat(message);
		}
	}

	private void handlePrivateUserChat(Message message, long userId) {
		if (allowedUserIds.contains(userId)) {
			privateChat(message);
		}
	}

	private void handleMyPrivateChat(Message message) {
		String inputText = message.getText();

		if (substitutionCommands.contains(inputText)) {
			handleSubstitutionCommands(inputText, message);
			return;
		}

		try {
			URI uri = getURL(parseUrlWithSign(inputText));
			if (isItHost(uri, YOUTUBE)) {
				if (shouldProcessYoutube(inputText)) {
					groupChat(message);
				}
			} else {
				groupChat(message);
			}
		} catch (InvalidUrlException | UnknownHostException ignored) {
		}
	}

	private boolean shouldProcessYoutube(String inputText) {
		return isSubstitutionEnabled.get() || inputText.startsWith("!");
	}

	private void handleSubstitutionCommands(String inputText, Message message) {
		if (substitutionCommandTrue.equals(inputText)) {
			sendByMessage("ок, обрабатываю ютуб", message);
			isSubstitutionEnabled.set(true);
		} else if (substitutionCommandFalse.equals(inputText)) {
			sendByMessage("ок, больше не обрабатываю ютуб", message);
			isSubstitutionEnabled.set(false);
		}
	}

	private void groupChat(Message message) {
		String inputText = message.getText();
		try {
			URI uri = getURL(parseUrlWithSign(inputText));
			logMessageWithUrl(message);
			String text = buildTextMessage(uri, message);
			jobService.runJob(uri, message, text, true);
			bot.deleteMessage(message);
		} catch (InvalidUrlException | UnknownHostException ignored) {
		} catch (NotSendException e) {
			log.error("Error sending message: {}", inputText, e);
		}
	}

	private void privateChat(Message message) {
		String inputText = message.getText();
		if (inputText.equals("/start")) {
			sendByMessage("Привет! Скачаю медиа по ссылке", message);
			return;
		}
		try {
			URI uri = getURL(inputText);
			logMessageWithUrl(message);
			jobService.runJob(uri, message, "", false);
		} catch (InvalidUrlException e) {
			sendByMessage("Какая-то неправильная у вас ссылка :(", message);
		} catch (UnknownHostException e) {
			sendByMessage("Неизвестный хост", message);
		}
	}

	private String buildTextMessage(URI uri, Message message) {
		try {
			User user = message.getFrom();
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

	private void logMessageWithUrl(Message message) {
		try {
			getURL(message.getText());
			log.info("New message in chat {} from {}: {}",
					message.getChatId(),
					message.getFrom().getId(),
					message.getText());
		} catch (InvalidUrlException | UnknownHostException ignored) {
		}
	}

	private void sendByMessage(String text, Message message) {
		BotUtils.sendByMessage(text, message, telegramClient);
	}
}
