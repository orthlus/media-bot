package main.social.ig;

import feign.Feign;
import feign.Request;
import feign.Response;
import feign.jackson.JacksonDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
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
				.decoder(new JacksonDecoder())
				.target(IGHttp.class, apiUrl);
	}

	public InputStream download(URI uri) {
		if (getMediaUrl(uri).isPresent()) {
			try (Response response = client.download(uri)) {
				return new ByteArrayInputStream(response.body().asInputStream().readAllBytes());
			} catch (IOException e) {
				throw new RuntimeException("error download ig file " + uri);
			}
		}

		throw new RuntimeException("error download ig file - empty uri by " + uri);
	}

	public Optional<String> getMediaUrl(URI uri) {
		IGMedia igMedia = client.mediaInfo(uri.toString(), apiToken);
		try {
			return Optional.of(parseSingleMediaUrl(igMedia));
		} catch (Exception e) {
			log.error("ig media url error", e);
			return Optional.empty();
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
