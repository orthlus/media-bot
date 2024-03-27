package main.social.ig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramService {
	private final RestTemplate client;

	public InputStream download(URI uri) {
		String mediaUrl = getMediaUrl(uri);
		byte[] bytes = client.getForObject(URI.create(mediaUrl), byte[].class);

		return new ByteArrayInputStream(bytes);
	}

	public String getMediaUrl(URI uri) {
		IGMedia igMedia = client.getForObject("/media/by/url?url=" + uri.toString(), IGMedia.class);
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
