package main.social.tiktok;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoDataBeta {
	@JsonProperty("data")
	private Data data;

	public List<String> getVideoUrls() {
		return data.itemInfo.itemStruct.video.bitrateInfo[0].playAddr.urlList;
	}

	static class Data {
		@JsonProperty("itemInfo")
		ItemInfo itemInfo;
	}

	static class ItemInfo {
		@JsonProperty("itemStruct")
		ItemStruct itemStruct;
	}

	static class ItemStruct {
		Video video;
	}

	static class Video {
		@JsonProperty("bitrateInfo")
		BitrateInfo[] bitrateInfo;
	}

	static class BitrateInfo {
		@JsonProperty("PlayAddr")
		PlayAddr playAddr;
	}

	static class PlayAddr {
		@JsonProperty("UrlList")
		List<String> urlList;
	}
}
