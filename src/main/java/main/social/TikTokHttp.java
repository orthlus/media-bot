package main.social;

import feign.Param;
import feign.RequestLine;
import feign.Response;

interface TikTokHttp {
	@RequestLine("GET /download?url={url}")
	Response download(@Param("url") String url);
}
