package main;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@SuppressWarnings("DataFlowIssue")
public class OkHttpUtils {
	public static String readBody(Response response) {
		try {
			String string = response.body().string();
			response.close();
			return string;
		} catch (IOException | NullPointerException e) {
			log.error("error getting body of response {}", response);
			return "";
		}
	}

	public static InputStream readInputStreamBody(Response response) {
		try {
			InputStream inputStream = response.body().byteStream();
			response.close();
			return inputStream;
		} catch (NullPointerException e) {
			log.error("error getting body of response {}", response);
			return InputStream.nullInputStream();
		}
	}

	public static byte[] readBinaryBody(Response response) {
		try {
			byte[] bytes = response.body().bytes();
			response.close();
			return bytes;
		} catch (NullPointerException | IOException e) {
			log.error("error getting body of response {}", response);
			return new byte[0];
		}
	}
}
