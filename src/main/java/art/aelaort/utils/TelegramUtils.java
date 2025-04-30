package art.aelaort.utils;

import art.aelaort.exceptions.TooLargeFileException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TelegramUtils {
	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static Update deserializeUpdate(String updateString) {
		try {
			return objectMapper.readValue(updateString, Update.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String serializeUpdate(Update update) {
		try {
			return objectMapper.writeValueAsString(update);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
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
