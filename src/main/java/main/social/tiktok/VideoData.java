package main.social.tiktok;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoData {
	@JsonProperty("aweme_list")
	private AwemeList[] awemeList;

	public List<String> getVideoUrls() {
		return awemeList[0].video.bitRate[0].playAddr.urlList;
	}

	static class AwemeList {
		@JsonProperty("video")
		Video video;
	}

	static class Video {
		@JsonProperty("bit_rate")
		BitRate[] bitRate;
	}

	static class BitRate {
		@JsonProperty("play_addr")
		PlayAddr playAddr;
	}

	static class PlayAddr {
		@JsonProperty("url_list")
		List<String> urlList;
	}
}
