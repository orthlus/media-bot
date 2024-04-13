package main.social;

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
				.setConnectTimeout(Duration.ofMinutes(5))
				.setReadTimeout(Duration.ofMinutes(5))
				.defaultHeader("x-access-key", igApiToken)
				.build();
	}

//	@Bean
	public RestTemplate tiktok(RestTemplateBuilder restTemplateBuilder,
							   @Value("${tiktok.api.url}") String tiktokApiUrl) {
		return restTemplateBuilder
				.rootUri(tiktokApiUrl)
				.setConnectTimeout(Duration.ofMinutes(5))
				.setReadTimeout(Duration.ofMinutes(5))
				.build();
	}

	@Bean
	public RestTemplate tiktokBeta(RestTemplateBuilder restTemplateBuilder,
							   @Value("${tiktok.beta.api.url}") String tiktokApiUrl) {
		return restTemplateBuilder
				.rootUri(tiktokApiUrl + "/api/v1/tiktok/web")
				.setConnectTimeout(Duration.ofMinutes(5))
				.setReadTimeout(Duration.ofMinutes(5))
				.build();
	}
}
