package art.aelaort.social;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class Config {
	@Bean
	public RestTemplate ig(RestTemplateBuilder restTemplateBuilder,
						   @Value("${instagram.api.token}") String igApiToken,
						   @Value("${instagram.api.url}") String igApiUrl) {
		return restTemplateBuilder
				.rootUri(igApiUrl)
				.connectTimeout(Duration.ofMinutes(5))
				.readTimeout(Duration.ofMinutes(5))
				.defaultHeader("x-access-key", igApiToken)
				.build();
	}

	@Bean
	public RestTemplate tiktok(RestTemplateBuilder restTemplateBuilder,
							   @Value("${tiktok.api.url}") String tiktokApiUrl,
							   @Value("${tiktok.api.token}") String tiktokApiToken) {
		return restTemplateBuilder
				.rootUri(tiktokApiUrl)
				.connectTimeout(Duration.ofMinutes(5))
				.readTimeout(Duration.ofMinutes(5))
				.defaultHeader("authorization", "Bearer " + tiktokApiToken)
				.build();
	}

	@Bean
	public RestTemplate ytdlp(RestTemplateBuilder restTemplateBuilder,
							  @Value("${ytdlp.url}") String url) {
		return restTemplateBuilder
				.rootUri(url)
				.connectTimeout(Duration.ofMinutes(5))
				.readTimeout(Duration.ofMinutes(5))
				.build();
	}
}
