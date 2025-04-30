package art.aelaort.dto.tiktok;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

class ImagePostInfo {
	@JsonProperty("images")
	List<Image> images;
}
