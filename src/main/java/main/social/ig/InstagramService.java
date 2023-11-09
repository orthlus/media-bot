package main.social.ig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Optional;

@Component
public class InstagramService {
	@Value("${instagram.api.token}")
	private String apiToken;
	@Value("${instagram.api.url}")
	private String apiUrl;


	public Optional<String> getMediaUrl(URI uri) {
		return null;
	}
}
