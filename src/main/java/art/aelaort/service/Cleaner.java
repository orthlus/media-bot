package art.aelaort.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Component
@ConditionalOnProperty(name = "run.mode", havingValue = "bot")
public class Cleaner {
	@Value("${ytdlp.dir}")
	private String ytdlpDir;

	@Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
	public void cleanup() {
		try (Stream<Path> walk = Files.walk(Path.of(ytdlpDir), 1)) {
			walk
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

	private boolean isFileOlder5Hours(Path path) {
		LocalDateTime fileCreateTime = getFileCreateTime(path);
		return LocalDateTime.now().minusHours(5).isAfter(fileCreateTime);
	}

	private LocalDateTime getFileCreateTime(Path path) {
		try {
			BasicFileAttributes basicFileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
			FileTime fileTime = basicFileAttributes.creationTime();
			return LocalDateTime.ofInstant(fileTime.toInstant(), Clock.systemDefaultZone().getZone());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
