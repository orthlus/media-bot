package art.aelaort.dto.tiktok;

import com.fasterxml.jackson.annotation.JsonProperty;

class Data {
	AwemeDetails[] awemeDetails;

	@JsonProperty("aweme_details")
	public void setAwemeDetailsArray(AwemeDetails[] awemeDetails) {
		this.awemeDetails = awemeDetails;
	}

	@JsonProperty("aweme_detail")
	public void setAwemeDetailsObject(AwemeDetails awemeDetail) {
		this.awemeDetails = new AwemeDetails[]{awemeDetail};
	}
}
