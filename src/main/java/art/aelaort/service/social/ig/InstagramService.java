package art.aelaort.service.social.ig;

import art.aelaort.dto.instagram.MediaUrl;
import art.aelaort.dto.instagram.PhotoUrl;
import art.aelaort.dto.instagram.VideoUrl;
import art.aelaort.dto.instagram.api.IGMedia;
import art.aelaort.exceptions.RequestIgUrlException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("DataFlowIssue")
@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramService {
	private final RestTemplate ig;
	private final RestTemplate igNoRedirect;

	@Retryable
	public InputStream download(MediaUrl url) {
		byte[] bytes = ig.getForObject(URI.create(url.getUrl()), byte[].class);
		return new ByteArrayInputStream(bytes);
	}

	public List<MediaUrl> getMediaUrls(URI uri) {
		log.debug("getMediaUrls({})", uri);
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
		} else if (uri.toString().contains("/share/")){
			Optional<String> redirectUri = tryGetRedirect(uri);
			if (redirectUri.isPresent()) {
				return requestMediaUrl("/v1/media/by/url?url=" + redirectUri);
			} else {
				return List.of();
			}
		} else {
			return requestMediaUrl("/v1/media/by/url?url=" + uri);
		}
	}

	@Retryable
	private List<MediaUrl> requestMediaUrl(String path) {
		try {
			IGMedia igMedia = ig.getForObject(path, IGMedia.class);
			return parseListMediaUrl(requireNonNull(igMedia));
		} catch (IllegalStateException | RestClientException e) {
			throw new RequestIgUrlException("ig error: " + path, e);
		}
	}

	private Optional<String> tryGetRedirect(URI uri) {
		log.debug("Trying to redirect to {}", uri);
		ResponseEntity<String> response = igNoRedirect.getForEntity(uri, String.class);
		log.debug("Redirected to {} - headers: {}", response.getStatusCode(), response.getHeaders());
		if (response.getStatusCode().is3xxRedirection()) {
			String location = response.getHeaders().getFirst("Location");
			log.debug("Redirected to {}", location);
			return Optional.ofNullable(location);
		}
		return Optional.empty();
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
