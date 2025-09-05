package art.aelaort.service.social.tiktok;

import art.aelaort.dto.tiktok.VideoData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
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
	public Path downloadFile(String fileUrl) {
        RestTemplate restTemplate = new RestTemplate();

        ResponseExtractor<Path> responseExtractor = response -> {
			File file = File.createTempFile("download", "tmp");
			StreamUtils.copy(response.getBody(), new FileOutputStream(file));
			return file.toPath();
		};

        return restTemplate.execute(URI.create(fileUrl), HttpMethod.GET, null, responseExtractor);
    }

	@Retryable
	public InputStream download(String fileUrl) {
		byte[] bytes = rawProxy.getForObject(URI.create(fileUrl), byte[].class);
		return new ByteArrayInputStream(bytes);
	}
}
