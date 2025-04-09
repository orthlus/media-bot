package art.aelaort.social.ig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import art.aelaort.exceptions.RequestIgUrlException;
import art.aelaort.social.ig.models.MediaUrl;
import art.aelaort.social.ig.models.PhotoUrl;
import art.aelaort.social.ig.models.VideoUrl;
import art.aelaort.social.ig.models.api.IGMedia;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramService {
	@Qualifier("ig")
	private final RestTemplate client;

	@Retryable
	public InputStream download(MediaUrl url) {
		log.info("trying download {}", url.getUrl());
		byte[] bytes = client.getForObject(URI.create(url.getUrl()), byte[].class);

		return new ByteArrayInputStream(bytes);
	}

	public List<MediaUrl> getMediaUrls(URI uri) {
		if (uri.toString().contains("/stories/")) {
			if (uri.toString().contains("/s/")) {
				try {
					return requestMediaUrl("/v1/share/by/url?url=" + uri);
				} catch (RequestIgUrlException e) {
					return requestMediaUrl("/v1/story/by/url?url=" + uri);
				}
			} else {
				return requestMediaUrl("/v1/story/by/url?url=" + uri);
			}
		} else {
			return requestMediaUrl("/v1/media/by/url?url=" + uri);
		}
	}

	@Retryable
	private List<MediaUrl> requestMediaUrl(String path) {
		try {
			IGMedia igMedia = client.getForObject(path, IGMedia.class);
			return parseListMediaUrl(requireNonNull(igMedia));
		} catch (IllegalStateException | RestClientException e) {
			throw new RequestIgUrlException("ig error: " + path, e);
		}
	}

	private List<MediaUrl> parseListMediaUrl(IGMedia media) {
		if (media.getMediaType() == 8) {
			return media.getResources().stream().map(this::parseSingleMediaUrl).toList();
		}
		return List.of(parseSingleMediaUrl(media));
	}

	private MediaUrl parseSingleMediaUrl(IGMedia media) {
		return switch (media.getMediaType()) {
			case 1 -> new PhotoUrl(media.getSinglePhotoUrl());
			case 2 -> new VideoUrl(media.getSingleVideoUrl());
			default -> throw new IllegalStateException("Unexpected value: " + media.getMediaType());
		};
	}
}
