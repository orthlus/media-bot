package art.aelaort.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "run.mode", havingValue = "job")
public class JobEntrypoint implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {

	}
}
