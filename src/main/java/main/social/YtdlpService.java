package main.social;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.exceptions.YtdlpFileDownloadException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class YtdlpService {
	@Qualifier("ytdlp")
	private final RestTemplate restTemplate;

	public int getVideoDurationSeconds(URI uri, String proxyUrl) {
		String formatted = "/video/duration?uri=%s&proxy_url=%s".formatted(uri, proxyUrl);
		String duration = restTemplate.getForObject(formatted, String.class);

		return duration == null ? -1 : Integer.parseInt(duration.trim());
	}

	public int getVideoDurationSeconds(URI uri) {
		String duration = restTemplate.getForObject("/video/duration?uri=" + uri, String.class);

		return duration == null ? -1 : Integer.parseInt(duration.trim());
	}

	public Path downloadFileByUrl(URI uri, String proxyUrl) {
		String formatted = "/download?uri=%s&proxy_url=%s".formatted(uri, proxyUrl);
		String filepath = restTemplate.getForObject(formatted, String.class);
		if (filepath == null) {
			throw new YtdlpFileDownloadException();
		} else {
			return Path.of(filepath.trim());
		}
	}

	public Path downloadFileByUrl(URI uri) {
		String filepath = restTemplate.getForObject("/download?uri=" + uri, String.class);
		if (filepath == null) {
			throw new YtdlpFileDownloadException();
		} else {
			return Path.of(filepath.trim());
		}
	}
}