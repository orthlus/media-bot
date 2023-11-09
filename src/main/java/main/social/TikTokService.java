package main.social;

import feign.Feign;
import feign.Request;
import feign.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TikTokService {
	@Value("${tiktok.url}")
	private String tiktokUrl;

	private TikTokHttp client;

	@PostConstruct
	private void init() {
		feign.Request.Options options = new Request.Options(5, TimeUnit.MINUTES, 5, TimeUnit.MINUTES, true);
		client = Feign.builder()
				.options(options)
				.target(TikTokHttp.class, tiktokUrl);
	}

	public InputStream download(URI url) {
		try(Response response = client.download(url.toString())) {
			InputStream inputStream = response.body().asInputStream();
			return new ByteArrayInputStream(inputStream.readAllBytes());
		} catch (IOException e) {
			log.error("http error - tiktok download", e);
			throw new RuntimeException("error during downloading tiktok file");
		}
	}
}
