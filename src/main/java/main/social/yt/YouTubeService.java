package main.social.yt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.Main;
import main.exceptions.YoutubeFileDownloadException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class YouTubeService {
	@Qualifier("ytdlp")
	private final RestTemplate restTemplate;
	@Value("${ytdlp.dir}")
	private String ytdlpDir;

	@Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
	public void cleanup() {
		try {
			Files.walk(Path.of(ytdlpDir), 1)
					.filter(path -> path.toFile().isFile())
					.filter(this::isFileOlder5Hours)
					.forEach(path -> {
						try {
							Files.deleteIfExists(path);
							log.info("deleted file {}", path);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public int getVideoDurationSeconds(URI uri) {
		String duration = restTemplate.getForObject("/video/duration?uri=" + uri, String.class);

		return duration == null ? -1 : Integer.parseInt(duration.trim());
	}

	public Path downloadFileByUrl(URI uri) {
		String filepath = restTemplate.getForObject("/download?uri=" + uri, String.class);
		if (filepath == null) {
			throw new YoutubeFileDownloadException();
		} else {
			return Path.of(filepath.trim());
		}
	}

	private boolean isFileOlder5Hours(Path path) {
		LocalDateTime fileCreateTime = getFileCreateTime(path);
		return LocalDateTime.now(Main.zone).minusHours(5).isAfter(fileCreateTime);
	}

	private LocalDateTime getFileCreateTime(Path path) {
		try {
			BasicFileAttributes basicFileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
			FileTime fileTime = basicFileAttributes.creationTime();
			return LocalDateTime.ofInstant(fileTime.toInstant(), Main.zone);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}