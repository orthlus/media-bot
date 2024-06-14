package main;

import main.exceptions.InvalidUrl;
import main.exceptions.UnknownHost;
import main.social.ig.KnownHosts;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class BotUtils {
	public static String parseUrlWithSign(String text) {
		return text.startsWith("!") ? text.substring(1) : text;
	}

	public static URI getURL(String url) throws InvalidUrl {
		try {
			return new URL(url).toURI();
		} catch (URISyntaxException | MalformedURLException e) {
			throw new InvalidUrl();
		}
	}

	public static boolean isItHost(URI uri, KnownHosts host) {
		return parseHost(uri).equals(host);
	}

	public static KnownHosts parseHost(URI uri) {
		String cleanedUrl = uri.getHost().replace("www.", "");
		for (KnownHosts knownHost : KnownHosts.values()) {
			for (String host : knownHost.getHosts()) {
				if (cleanedUrl.contains(host)) {
					return knownHost;
				}
			}
		}
		throw new UnknownHost();
	}
}
