package art.aelaort.config;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.time.Duration;

@Configuration
@EnableRetry
public class Config {
	@Value("${proxy-url}")
	private URI proxyUrl;

	private void proxyCustomizer(RestTemplate restTemplate) {
		String host = proxyUrl.getHost();
		int port = proxyUrl.getPort();

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));

		requestFactory.setProxy(proxy);
		restTemplate.setRequestFactory(requestFactory);
	}

	@Bean
	public RestTemplate igNoRedirect(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder
				.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(
						HttpClients.custom()
								.disableRedirectHandling()
								.build()))
				.customizers(this::proxyCustomizer)
				.connectTimeout(Duration.ofSeconds(20))
				.readTimeout(Duration.ofSeconds(20))
				.build();
	}

	@Bean
	public RestTemplate ig(RestTemplateBuilder restTemplateBuilder,
						   @Value("${instagram.api.token}") String igApiToken,
						   @Value("${instagram.api.url}") String igApiUrl) {
		return restTemplateBuilder
				.customizers(this::proxyCustomizer)
				.rootUri(igApiUrl)
				.connectTimeout(Duration.ofSeconds(20))
				.readTimeout(Duration.ofSeconds(20))
				.defaultHeader("x-access-key", igApiToken)
				.build();
	}

	@Bean
	public RestTemplate tiktok(RestTemplateBuilder restTemplateBuilder,
							   @Value("${tiktok.api.url}") String tiktokApiUrl,
							   @Value("${tiktok.api.token}") String tiktokApiToken) {
		return restTemplateBuilder
//				.customizers(this::proxyCustomizer)
				.rootUri(tiktokApiUrl)
				.connectTimeout(Duration.ofSeconds(20))
				.readTimeout(Duration.ofSeconds(20))
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

	@Bean
	public RestTemplate raw(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder
				.connectTimeout(Duration.ofMinutes(5))
				.readTimeout(Duration.ofMinutes(5))
				.build();
	}

	@Bean
	public RestTemplate rawProxy(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder
				.customizers(this::proxyCustomizer)
				.connectTimeout(Duration.ofMinutes(5))
				.readTimeout(Duration.ofMinutes(5))
				.build();
	}
}
