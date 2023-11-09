package main;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.Map;

public interface TelegramApiHttp {
	@RequestLine("POST /bot{token}/setWebhook")
	@Headers("Content-Type: application/x-www-form-urlencoded")
	void register(@Param("token") String token, Map<String, ?> params);
}
