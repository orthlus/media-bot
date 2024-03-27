package main.social.ig;

import feign.Feign;
import feign.Request;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class InstagramService {
	@Value("${instagram.api.token}")
	private String apiToken;
	@Value("${instagram.api.url}")
	private String apiUrl;

	private IGHttp client;

	@PostConstruct
	private void init() {
		feign.Request.Options options = new Request.Options(5, TimeUnit.MINUTES, 5, TimeUnit.MINUTES, true);
		client = Feign.builder()
				.options(options)
				.decoder((response, type) ->
						byte[].class.equals(type) ?
								new Decoder.Default().decode(response, type) :
								new JacksonDecoder().decode(response, type))
				.target(IGHttp.class, apiUrl);
	}

	public InputStream download(URI uri) {
		String mediaUrl = getMediaUrl(uri);
		byte[] bytes = client.download(URI.create(mediaUrl));

		return new ByteArrayInputStream(bytes);
	}

	public String getMediaUrl(URI uri) {
		IGMedia igMedia = client.mediaInfo(uri.toString(), apiToken);
		try {
			return parseSingleMediaUrl(igMedia);
		} catch (Exception e) {
			log.error("ig media url error", e);

			throw new RuntimeException("ig media url error " + uri);
		}
	}

	private String parseSingleMediaUrl(IGMedia media) {
		return switch (media.getMediaType()) {
			case 1 -> media.getSinglePhotoUrl();
			case 2 -> media.getSingleVideoUrl();
			default -> throw new IllegalStateException("Unexpected value: " + media.getMediaType());
		};
	}
}
