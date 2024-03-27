package main.social;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class Config {
	@Value("${instagram.api.token}")
	private String igApiToken;
	@Value("${instagram.api.url}")
	private String igApiUrl;

	@Bean
	public RestTemplate ig(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder
				.rootUri(igApiUrl)
				.setConnectTimeout(Duration.ofMinutes(5))
				.setReadTimeout(Duration.ofMinutes(5))
				.defaultHeader("x-access-key", igApiToken)
				.build();
	}
}
