package main.social.ig;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.net.URI;

public interface IGHttp {
	@RequestLine("GET /media/by/url?url={url}")
	@Headers("x-access-key: {token}")
	IGMedia mediaInfo(@Param("url") String url, @Param("token") String token);

	@RequestLine("GET")
	byte[] download(URI uri);
}
