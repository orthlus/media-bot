package art.aelaort.service;

import art.aelaort.dto.processing.JobData;
import art.aelaort.utils.TelegramUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "run.mode", havingValue = "job")
public class JobEntrypoint implements CommandLineRunner {
	@Value("${job_data}")
	private String jobDataString;

	@Override
	public void run(String... args) throws Exception {
		JobData jobData = TelegramUtils.deserializeJobData(jobDataString);

	}
}
