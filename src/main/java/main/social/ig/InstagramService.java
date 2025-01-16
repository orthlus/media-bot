package main.social.ig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.social.ig.models.MediaUrl;
import main.social.ig.models.PhotoUrl;
import main.social.ig.models.VideoUrl;
import main.social.ig.models.api.IGMedia;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramService {
	@Qualifier("ig")
	private final RestTemplate client;

	public InputStream download(MediaUrl url) {
		byte[] bytes = client.getForObject(URI.create(url.getUrl()), byte[].class);

		return new ByteArrayInputStream(bytes);
	}

	public MediaUrl getMediaUrl(URI uri) {
		IGMedia igMedia = client.getForObject("/v1/media/by/url?url=" + uri.toString(), IGMedia.class);
		try {
			return parseSingleMediaUrl(igMedia);
		} catch (Exception e) {
			log.error("ig media url error", e);

			throw new RuntimeException("ig media url error " + uri);
		}
	}

	private MediaUrl parseSingleMediaUrl(IGMedia media) {
		return switch (media.getMediaType()) {
			case 1 -> new PhotoUrl(media.getSinglePhotoUrl());
			case 2 -> new VideoUrl(media.getSingleVideoUrl());
			default -> throw new IllegalStateException("Unexpected value: " + media.getMediaType());
		};
	}
}
