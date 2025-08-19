package art.aelaort.dto.tiktok;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

class Data {
	@JsonProperty("aweme_details")
	@JsonAlias("aweme_detail")
	AwemeDetails[] awemeDetails;
}
