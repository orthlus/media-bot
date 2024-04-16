package main.social.tiktok;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

@SuppressWarnings("DataFlowIssue")
@Slf4j
@Component
@RequiredArgsConstructor
public class TikTokBetaService implements TikTok {
	@Qualifier("tiktokBeta")
	private final RestTemplate client;

	@Override
	public List<String> getMediaUrls(URI videoUrl) {
		String videoId = getVideoId(videoUrl);
		return getMediaUrls0(videoId);
	}

	private List<String> getMediaUrls0(String videoId) {
		return client
				.getForObject("/fetch_post_detail?itemId=" + videoId, VideoDataBeta.class)
				.getVideoUrls();
	}

	private String getVideoId(URI videoUrl) {
		return client
				.getForObject("/get_aweme_id?url=" + videoUrl.toString(), VideoIdResponse.class)
				.id;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class VideoIdResponse {
		@JsonProperty("data")
		String id;
	}

	@Override
	public InputStream download(String videoUrl) {
		byte[] bytes = client.getForObject(URI.create(videoUrl), byte[].class);
		return new ByteArrayInputStream(bytes);
	}

	@Override
	public String getTiktokServiceName() {
		return "tikhub-beta";
	}

}
