package art.aelaort.dto.processing;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.net.URI;

public record JobData(String type, URI uri, Update update, String text, boolean isDeleteSourceMessage) {
}
