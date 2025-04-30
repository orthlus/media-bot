package art.aelaort.service.social.ig.models.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class IGMedia {
	@JsonProperty("media_type") // 1, 2, 8
	int mediaType;
	@JsonProperty("thumbnail_url")
	String singlePhotoUrl;
	@JsonProperty("video_url")
	String singleVideoUrl;
	@JsonProperty("resources") // media_type = 8
	List<IGMedia> resources;
}
