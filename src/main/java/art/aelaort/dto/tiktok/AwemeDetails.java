package art.aelaort.dto.tiktok;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class AwemeDetails {
	@JsonProperty("video")
	Video video;
	@JsonProperty("image_post_info")
	ImagePostInfo imagePostInfo;
}
