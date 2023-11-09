package main;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BotController {
	private final BotHandler bot;
	@Value("${bot.secret}")
	private String botSecret;

	@PostMapping("/telegram/bot")
	public ResponseEntity<BotApiMethod<?>> update(@RequestBody Update update, HttpServletRequest request) {
		if (validSecret(request)) {
			bot.onWebhookUpdateReceived(update);
		}

		return ResponseEntity.ok().build();
	}

	private boolean validSecret(HttpServletRequest request) {
		try {
			String secret = getSecret(request);
			String botStoredSecret = botSecret;

			if (botStoredSecret == null) throw new RuntimeException("telegram secret not stored");
			if (!secret.equals(botStoredSecret)) throw new RuntimeException("invalid telegram secret");

			return true;
		} catch (Exception e) {
			log.error("telegram valid secret error", e);
		}

		return false;
	}

	private String getSecret(HttpServletRequest request) {
		String secret = request.getHeader("X-Telegram-Bot-Api-Secret-Token");

		if (secret != null)
			return secret;
		else
			throw new RuntimeException("telegram secret not found");
	}
}
