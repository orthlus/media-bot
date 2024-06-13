package main.social.tiktok;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

@SuppressWarnings("DataFlowIssue")
@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class TikTokService implements TikTok {
	@Qualifier("tiktok")
	private final RestTemplate client;

	@Override
	public List<String> getMediaUrls(URI videoUrl) {
		return getMediaUrlsObject(videoUrl).getVideoUrls();
	}

	private VideoData getMediaUrlsObject(URI videoUrl) {
		String url = "/api/v1/tiktok/app/v3/fetch_one_video_by_share_url?share_url=" + videoUrl.toString();
		return client.getForObject(url, VideoData.class);
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
}
