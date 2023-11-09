package main.social;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static main.OkHttpUtils.readBody;
import static main.OkHttpUtils.readInputStreamBody;

@Slf4j
@Component
@RequiredArgsConstructor
public class TikTokService {
	@Value("${tiktok.url}")
	private String tiktokUrl;

	private final OkHttpClient httpClient = new OkHttpClient.Builder()
			.callTimeout(5, TimeUnit.MINUTES)
			.connectTimeout(5, TimeUnit.MINUTES)
			.readTimeout(5, TimeUnit.MINUTES)
			.build();

	public InputStream download(URI url) {
		Request request = new Request.Builder().get()
				.url(tiktokUrl + "/download?url=" + url)
				.build();
		Call call = httpClient.newCall(request);
		try {
			Response response = call.execute();
			if (response.isSuccessful()) {
				return readInputStreamBody(response);
			}
			log.error("http error - tiktok download - response code - {}, body - {}", response.code(), readBody(response));
		} catch (IOException e) {
			log.error("http error - tiktok download", e);
		}
		throw new RuntimeException("error during downloading tiktok file");
	}
}
