package main;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class BotConfig {
	@Value("${bot.nickname}")
	private String nickname;
	@Value("${bot.token}")
	private String token;
}
