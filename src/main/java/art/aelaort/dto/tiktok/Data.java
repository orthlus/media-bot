package art.aelaort.dto.tiktok;

import com.fasterxml.jackson.annotation.JsonProperty;

class Data {
	@JsonProperty("aweme_details")
	AwemeDetails[] awemeDetails;
}
