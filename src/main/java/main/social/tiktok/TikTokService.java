package main.social.tiktok;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DataFlowIssue")
@Slf4j
@Order(1)
//@Component
@RequiredArgsConstructor
public class TikTokService implements TikTok {
	@Qualifier("tiktok")
	private final RestTemplate client;
	@Value("${tiktok.api.username}")
	private String username;
	@Value("${tiktok.api.password}")
	private String password;
	private String token;

//	@PostConstruct
	public void init() {
		updateToken();
	}

	public void updateToken() {
		token = getToken();
	}

	private String getToken() {
		return client.postForObject(
				"/user/login?token_expiry_minutes=525600",
				new HttpEntity<>(
						new LinkedMultiValueMap<>() {{
							add("username", username);
							add("password", password);
						}},
						new HttpHeaders() {{
							setContentType(MediaType.APPLICATION_FORM_URLENCODED);
						}}
				),
				TiktokTokenDto.class
		).getAccessToken();
	}

	public HttpEntity<?> getTokenEntity() {
		return new HttpEntity<>(
				new HttpHeaders() {{
					setBearerAuth(token);
				}}
		);
	}

	@Override
	public List<String> getMediaUrls(URI videoUrl) {
		try {
			return getMediaUrls0(videoUrl).getVideoUrls();
		} catch (HttpClientErrorException e) {
			if (e.getResponseBodyAsString().contains("Account subscription has expired")) {
				checkIn();
				sleep();

				return getMediaUrls0(videoUrl).getVideoUrls();
			} else {
				throw e;
			}
		}
	}

	private VideoData getMediaUrls0(URI videoUrl) {
		return client.exchange(
						"/tiktok/video_data/?tiktok_video_url=" + videoUrl.toString(),
						HttpMethod.GET,
						getTokenEntity(),
						VideoData.class)
				.getBody();
	}

	@Override
	public InputStream download(String videoUrl) {
		byte[] bytes = client.getForObject(URI.create(videoUrl), byte[].class);
		return new ByteArrayInputStream(bytes);
	}

	@Override
	public String getTiktokServiceName() {
		return "tikhub";
	}

	private void sleep() {
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void checkIn() {
		Response resp = client.exchange(
				"/promotion/daily_check_in",
				HttpMethod.GET,
				getTokenEntity(),
				Response.class
		).getBody();
		log.info("tiktok checkin - {}", resp.message);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static class Response {
		@JsonProperty("message")
		String message;
	}
}
