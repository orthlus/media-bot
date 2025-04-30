package art.aelaort.dto.tiktok;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

class DisplayImage {
	@JsonProperty("url_list")
	List<String> urlList;
}
