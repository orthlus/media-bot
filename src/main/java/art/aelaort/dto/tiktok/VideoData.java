package art.aelaort.dto.tiktok;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoData {
	@JsonProperty("data")
	private Data data;

	public List<String> getVideoUrls() {
		try {
			return data.awemeDetails.get(0).video.playAddr.urlList;
		} catch (NullPointerException e) {
			return data.awemeDetails.get(0).video.bitRate[0].playAddr.urlList;
		}
	}

	public List<String> getImagesUrls() {
		List<Image> images = data.awemeDetails.get(0).imagePostInfo.images;
		return images.stream()
				.map(image -> image.displayImage.urlList.get(0))
				.toList();
	}

	public boolean hasImages() {
		return data.awemeDetails.get(0).imagePostInfo != null;
	}
}
