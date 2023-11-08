package main;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Component
@RestController
@RequestMapping("youtube")
@RequiredArgsConstructor
public class YouTubeController {
	private final YouTubeService service;

	@GetMapping("download")
	public byte[] download(@RequestParam String url) throws IOException {
		return service.downloadByUrl(url);
	}
}
