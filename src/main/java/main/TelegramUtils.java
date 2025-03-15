package main;

import lombok.SneakyThrows;
import main.exceptions.TooLargeFileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TelegramUtils {
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
