package main.social;

import feign.Param;
import feign.RequestLine;

interface TikTokHttp {
	@RequestLine("GET /download?url={url}")
	byte[] download(@Param("url") String url);
}
