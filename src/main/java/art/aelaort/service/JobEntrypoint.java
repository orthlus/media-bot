package art.aelaort.service;

import art.aelaort.dto.processing.JobData;
import art.aelaort.utils.TelegramUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "run.mode", havingValue = "job")
public class JobEntrypoint implements CommandLineRunner {
	private final SocialHandlerService socialHandlerService;
	private final ApplicationContext applicationContext;
	@Value("${job_data}")
	private String jobDataString;

	@Override
	public void run(String... args) throws Exception {
		JobData jobData = TelegramUtils.deserializeJobData(jobDataString);
		socialHandlerService.handleByHost(
				jobData.uri(),
				jobData.update(),
				jobData.text(),
				jobData.isDeleteSourceMessage()
		);
		System.exit(SpringApplication.exit(applicationContext, () -> 0));
	}
}
