package art.aelaort.dto.tiktok;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
		List<String> result = new ArrayList<>(images.size());

		for (Image image : images) {
			List<String> urlList = image.displayImage.urlList;
			Optional<String> url = urlList.stream()
					.filter(u -> !u.contains(".heic"))
					.findFirst();
			url.ifPresent(result::add);
		}
		return result;
	}

	public boolean hasImages() {
		return data.awemeDetails.get(0).imagePostInfo != null;
	}
}
