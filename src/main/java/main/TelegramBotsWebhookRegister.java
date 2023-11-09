package main;

import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.ApiConstants;

@Slf4j
@Component
public class TelegramBotsWebhookRegister implements InitializingBean {
	private final OkHttpClient client = new OkHttpClient();
	private final BotConfig config;
	private final String webhookUrl;

	public TelegramBotsWebhookRegister(@Value("${webhook.url}") String webhookUrl, BotConfig config) {
		this.config = config;
		this.webhookUrl = webhookUrl;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			Request request = request(config, webhookUrl);
			client.newCall(request).execute().body().close();
		} catch (NullPointerException ignored) {
		}
		log.info("bot {} webhook registered", config.getNickname());
	}

	private Request request(BotConfig bot, String webhookUrl) {
		String url = "%s%s/setWebhook".formatted(ApiConstants.BASE_URL, bot.getToken());
		FormBody body = new FormBody.Builder()
				.add("url", webhookUrl)
				.add("drop_pending_updates", "True")
				.build();
		return new Request.Builder().url(url).post(body).build();
	}
}
