package main;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.system.Response;
import main.system.SystemProcess;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class YouTubeService {
	private final SystemProcess system;

	public byte[] downloadByUrl(String url) throws IOException {
		Optional<Path> fileOp = downloadByUrl(url, "/tmp");
		if (fileOp.isPresent()) {
			byte[] bytes = Files.readAllBytes(fileOp.get());
			Files.deleteIfExists(fileOp.get());

			return bytes;
		}
		return new byte[0];
	}

	private Optional<Path> downloadByUrl(String url, String dir) {
		String command = "yt-dlp --print filename -S res:1080 -P " + dir + " -o 'video-id-%(id)s.%(ext)s' --no-simulate " + url;
		Response response = system.callProcess(command);
		if (response.exitCode() != 0 || response.stderr().length() > 0) {
			log.error("Error downloading youtube {}", response);

			return Optional.empty();
		}

		Path path = Path.of(response.stdout());
		if (Files.exists(path)) {
			return Optional.of(path);
		} else {
			throw new RuntimeException();
		}
	}
}