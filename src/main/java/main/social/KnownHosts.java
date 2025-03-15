package main.social;

import lombok.Getter;

@Getter
public enum KnownHosts {
	INSTAGRAM("instagram", "instagram.com"),
	TIKTOK("tiktok", "tiktok.com", "vt.tiktok.com", "vm.tiktok.com"),
	VK("vk", "vk.com"),
	YOUTUBE("youtube", "youtube.com", "youtu.be");
	final String[] hosts;
	final String text;

	KnownHosts(String text, String... hosts) {
		this.text = text;
		this.hosts = hosts;
	}
}
