package main;

import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TelegramBotsWebhookRegister implements InitializingBean {
	private final OkHttpClient client = new OkHttpClient();
	private final BotConfig config;
	private final String webhookUrl;
	private final boolean webhookNeedRegister;
	private final String telegramApiUrl;
	private final String botSecret;

	public TelegramBotsWebhookRegister(@Value("${webhook.url}") String webhookUrl,
									   @Value("${telegram.api.url}") String telegramApiUrl,
									   @Value("${webhook.need_register}") boolean webhookNeedRegister,
									   @Value("${bot.secret}") String botSecret,
									   BotConfig config) {
		this.config = config;
		this.webhookUrl = webhookUrl;
		this.webhookNeedRegister = webhookNeedRegister;
		this.telegramApiUrl = telegramApiUrl;
		this.botSecret = botSecret;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (webhookNeedRegister) {
			try {
				Request request = request(config, webhookUrl);
				client.newCall(request).execute().body().close();
			} catch (NullPointerException ignored) {
			}
			log.info("bot {} webhook {} registered", config.getNickname(), webhookUrl);
		}
	}

	private Request request(BotConfig bot, String webhookUrl) {
		String url = "%s%s/setWebhook".formatted(telegramApiUrl, bot.getToken());
		FormBody body = new FormBody.Builder()
				.add("url", webhookUrl)
				.add("secret_token", botSecret)
				.add("drop_pending_updates", "True")
				.build();
		return new Request.Builder().url(url).post(body).build();
	}
}
