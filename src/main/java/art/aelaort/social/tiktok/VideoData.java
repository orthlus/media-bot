package art.aelaort.social.tiktok;


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

	public List<String> getImagesUrls() {
		List<Image> images = data.awemeDetails[0].imagePostInfo.images;
		return images.stream()
				.map(image -> image.displayImage.urlList.get(1))
				.toList();
	}

	public boolean hasImages() {
		return data.awemeDetails[0].imagePostInfo != null;
	}

	static class Data {
		@JsonProperty("aweme_details")
		private AwemeDetails[] awemeDetails;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static class AwemeDetails {
		@JsonProperty("video")
		Video video;
		@JsonProperty("image_post_info")
		ImagePostInfo imagePostInfo;
	}

	static class ImagePostInfo {
		@JsonProperty("images")
		List<Image> images;
	}

	static class Image {
		@JsonProperty("display_image")
		DisplayImage displayImage;
	}

	static class DisplayImage {
		@JsonProperty("url_list")
		List<String> urlList;
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
