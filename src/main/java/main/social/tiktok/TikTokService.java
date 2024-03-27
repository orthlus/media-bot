package main.social.tiktok;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TikTokService {
	@Qualifier("tiktok")
	private final RestTemplate client;

	public List<String> getMediaUrls(URI videoUrl) {
		String url = "/tiktok/video_data/?tiktok_video_url=" + videoUrl.toString();
		log.info("tiktok getMediaUrls {}", url);
		VideoData videoData = client.getForObject(url, VideoData.class);
		return videoData.getVideoUrls();
	}

	public InputStream download(String videoUrl) {
		byte[] bytes = client.getForObject(URI.create(videoUrl), byte[].class);
		return new ByteArrayInputStream(bytes);
	}
}
