package main;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static main.OkHttpUtils.readBinaryBody;
import static main.OkHttpUtils.readBody;

@Slf4j
@Component
@RestController
@RequestMapping("tiktok")
@RequiredArgsConstructor
public class TikTokController {
	@Value("${tiktok.url}")
	private String tiktokUrl;

	private final OkHttpClient httpClient = new OkHttpClient.Builder()
			.callTimeout(5, TimeUnit.MINUTES)
			.connectTimeout(5, TimeUnit.MINUTES)
			.readTimeout(5, TimeUnit.MINUTES)
			.build();

	@GetMapping("download")
	public byte[] download(@RequestParam String url) {
		Request request = new Request.Builder().get()
				.url(tiktokUrl + "/download?url=" + url)
				.build();
		Call call = httpClient.newCall(request);
		try {
			Response response = call.execute();
			if (response.isSuccessful()) {
				return readBinaryBody(response);
			}
			log.error("http error - tiktok download - response code - {}, body - {}", response.code(), readBody(response));
		} catch (IOException e) {
			log.error("http error - tiktok download", e);
		}
		throw new RuntimeException("error during downloading tiktok file");
	}
}
