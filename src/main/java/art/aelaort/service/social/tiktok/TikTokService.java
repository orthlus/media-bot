package art.aelaort.service.social.tiktok;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Retryable;
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
public class TikTokService {
	@Qualifier("tiktok")
	private final RestTemplate client;
	private final RestTemplate rawProxy;

	@Retryable
	public VideoData getData(URI videoUrl) {
		String url = "/api/v1/tiktok/app/v3/fetch_one_video_by_share_url?share_url=" + videoUrl.toString();
		return client.getForObject(url, VideoData.class);
	}

	public boolean isVideo(VideoData data) {
		return !data.hasImages();
	}

	public List<String> getImagesUrls(VideoData data) {
		return !isVideo(data) ? data.getImagesUrls() : List.of();

	}

	public List<String> getVideoMediaUrls(VideoData data) {
		return isVideo(data) ? data.getVideoUrls() : List.of();

	}

	@Retryable
	public InputStream download(String videoUrl) {
		byte[] bytes = rawProxy.getForObject(URI.create(videoUrl), byte[].class);
		return new ByteArrayInputStream(bytes);
	}
}
