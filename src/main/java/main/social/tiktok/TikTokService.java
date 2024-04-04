package main.social.tiktok;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DataFlowIssue")
@Slf4j
@Component
@RequiredArgsConstructor
public class TikTokService {
	@Qualifier("tiktok")
	private final RestTemplate client;
	@Value("${tiktok.api.username}")
	private String username;
	@Value("${tiktok.api.password}")
	private String password;
	private String token;

	@PostConstruct
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

	public List<String> getMediaUrls(URI videoUrl) throws InterruptedException {
		ResponseEntity<VideoData> response = getMediaUrls0(videoUrl);
		if (response.getStatusCode().is4xxClientError()) {
			checkIn();
			TimeUnit.SECONDS.sleep(5);
			response = getMediaUrls0(videoUrl);
		}
		return response
				.getBody()
				.getVideoUrls();
	}

	private ResponseEntity<VideoData> getMediaUrls0(URI videoUrl) {
		return client.exchange(
				"/tiktok/video_data/?tiktok_video_url=" + videoUrl.toString(),
				HttpMethod.GET,
				getTokenEntity(),
				VideoData.class);
	}

	public InputStream download(String videoUrl) {
		byte[] bytes = client.getForObject(URI.create(videoUrl), byte[].class);
		return new ByteArrayInputStream(bytes);
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
