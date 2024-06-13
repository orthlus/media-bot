package main.social.tiktok;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoData {
	@JsonProperty("data")
	private Data data;

	public List<String> getVideoUrls() {
		return data.awemeDetails[0].video.bitRate[0].playAddr.urlList;
	}

	static class Data {
		@JsonProperty("aweme_details")
		private AwemeDetails[] awemeDetails;
	}

	static class AwemeDetails {
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
