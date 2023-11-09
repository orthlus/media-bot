package main;

public enum KnownHosts {
	INSTAGRAM("instagram.com"),
	TIKTOK("tiktok.com", "vt.tiktok.com", "vm.tiktok.com"),
	YOUTUBE("youtube.com", "youtu.be");
	final String[] hosts;

	KnownHosts(String... hosts) {
		this.hosts = hosts;
	}
}
