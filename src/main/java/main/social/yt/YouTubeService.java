package main.social.yt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.Main;
import main.exceptions.YoutubeFileDownloadException;
import main.system.Response;
import main.system.SystemProcess;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class YouTubeService {
	private final SystemProcess system;
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

		return duration == null ? -1 : Integer.parseInt(duration);
	}

	public Path downloadFileByUrl(URI uri) {
		String filepath = restTemplate.getForObject("/download?uri=" + uri, String.class);
		if (filepath == null) {
			throw new YoutubeFileDownloadException();
		} else {
			return Path.of(filepath.trim());
		}
	}

	public InputStream downloadByUrl(URI uri) throws IOException {
		Optional<Path> fileOp = downloadByUrl(uri, "/tmp");
		if (fileOp.isPresent()) {
			byte[] bytes = Files.readAllBytes(fileOp.get());
			Files.deleteIfExists(fileOp.get());

			return new ByteArrayInputStream(bytes);
		}
		return ByteArrayInputStream.nullInputStream();
	}

	private Optional<Path> downloadByUrl(URI uri, String dir) {
		String command = "yt-dlp --print filename -S res:1080 -P " + dir + " -o video-id-%(id)s.%(ext)s --no-simulate " + uri;
		Response response = system.callProcess(command);
		if (response.exitCode() != 0) {
			log.error("Error downloading youtube {}", response);

			return Optional.empty();
		}

		Path path = Path.of(response.stdout());
		if (Files.exists(path)) {
			return Optional.of(path);
		} else {
			log.error("download error stdout: {}, stderr: {}", response.stdout(), response.stderr());
			throw new RuntimeException();
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