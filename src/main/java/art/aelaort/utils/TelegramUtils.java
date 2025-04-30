package art.aelaort.utils;

import art.aelaort.exceptions.TooLargeFileException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TelegramUtils {
	private static final ObjectMapper objectMapper = new ObjectMapper();

	@SneakyThrows
	public static Update deserializeUpdate(String updateString) {
		return objectMapper.readValue(updateString, Update.class);
	}

	@SneakyThrows
	public static String serializeUpdate(Update update) {
		return objectMapper.writeValueAsString(update);
	}

	public static void checkFileSize(Path file) throws IOException {
		if (isFileTooLarge(file)) {
			Files.deleteIfExists(file);
			throw new TooLargeFileException();
		}
	}

	@SneakyThrows
	private static boolean isFileTooLarge(Path file) {
		return Files.size(file) > 2L * 1024 * 1024 * 1024;
	}
}
