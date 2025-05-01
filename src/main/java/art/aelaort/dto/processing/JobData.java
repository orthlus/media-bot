package art.aelaort.dto.processing;

import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.net.URI;

public record JobData(URI uri, Message message, String text, boolean isDeleteSourceMessage) {
}
