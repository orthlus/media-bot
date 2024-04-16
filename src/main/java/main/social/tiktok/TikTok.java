package main.social.tiktok;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

public interface TikTok {
	List<String> getMediaUrls(URI videoUrl);
	InputStream download(String videoUrl);
	String getTiktokServiceName();
}
