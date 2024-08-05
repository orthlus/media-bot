package main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.ZoneId;

@SpringBootApplication
@EnableScheduling
public class Main {
	public static final ZoneId zone = ZoneId.of("Europe/Moscow");
	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
}
