package art.aelaort.dto.tiktok;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;

class Data {
	List<AwemeDetails> awemeDetails;

	@JsonProperty("aweme_details")
	public void setAwemeDetailsArray(AwemeDetails[] awemeDetails) {
		this.awemeDetails = Arrays.asList(awemeDetails);
	}

	@JsonProperty("aweme_detail")
	public void setAwemeDetailsObject(AwemeDetails awemeDetail) {
		this.awemeDetails = List.of(awemeDetail);
	}
}
