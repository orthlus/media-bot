package main.social.tiktok;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiDailyCheckIn {
	@Qualifier("tiktok")
	private final RestTemplate client;

//	@Scheduled(cron = "0 0 17 * * *", zone = "Europe/Moscow")
	public void checkIn() {
		Response resp = client.getForObject("/promotion/daily_check_in", Response.class);
		log.info("tiktok daily checkin - {}", resp.message);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static class Response {
		@JsonProperty("messsage")
		String message;
	}
}
