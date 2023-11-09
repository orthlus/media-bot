package main;

import feign.Feign;
import feign.form.FormEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class TelegramBotsWebhookRegister implements InitializingBean {
	private final TelegramApiHttp client;
	private final BotConfig config;
	private final String webhookUrl;
	private final boolean webhookNeedRegister;
	private final String botSecret;

	public TelegramBotsWebhookRegister(@Value("${webhook.url}") String webhookUrl,
									   @Value("${telegram.api.url}") String telegramApiUrl,
									   @Value("${webhook.need_register}") boolean webhookNeedRegister,
									   @Value("${bot.secret}") String botSecret,
									   BotConfig config) {
		this.config = config;
		this.webhookUrl = webhookUrl;
		this.webhookNeedRegister = webhookNeedRegister;
		this.botSecret = botSecret;
		client = Feign.builder()
				.encoder(new FormEncoder())
				.target(TelegramApiHttp.class, telegramApiUrl);
	}

	@Override
	public void afterPropertiesSet() {
		if (webhookNeedRegister) {
			client.register(config.getToken(), params(webhookUrl, botSecret, true));
			log.info("bot {} webhook {} registered", config.getNickname(), webhookUrl);
		}
	}

	private Map<String, ?> params(String url, String secretToken, boolean dropPendingUpdates) {
		return Map.of(
				"url", url,
				"secret_token", secretToken,
				"drop_pending_updates", dropPendingUpdates
		);
	}
}
