package art.aelaort.dto.tiktok;

import com.fasterxml.jackson.annotation.JsonProperty;

class Video {
	@JsonProperty("bit_rate")
	BitRate[] bitRate;
	@JsonProperty("play_addr")
	PlayAddr playAddr;
}
