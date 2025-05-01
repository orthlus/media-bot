package art.aelaort.service;

import art.aelaort.dto.processing.JobData;
import art.aelaort.utils.TelegramUtils;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "run.mode", havingValue = "bot")
public class JobService {
	private final ResourceLoader resourceLoader;

	public void runJob(URI uri, Update update, String text, boolean isDeleteSourceMessage) {
		try (KubernetesClient client = new KubernetesClientBuilder().build()) {
			Job job = parseJob("job.yaml");

			JobData jobData = new JobData(uri, update, text, isDeleteSourceMessage);
			String jobDataStr = TelegramUtils.serializeJobData(jobData);

			List<EnvVar> env = job.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
			env.add(envVar("run_mode", "job"));
			env.add(envVar("job_data", jobDataStr));
			job.getMetadata().setName(UUID.randomUUID().toString());

			client.batch().v1().jobs()
//					.inNamespace("default")
					.resource(job).create();
			System.out.println("Job from YAML created successfully!");
		}
	}


	private EnvVar envVar(String name, String value) {
		return new EnvVarBuilder()
				.withName(name)
				.withValue(value)
				.build();
	}

	private Job parseJob(String ymlFile) {
		try {
			Resource resource = resourceLoader.getResource("classpath:" + ymlFile);
			return Serialization.unmarshal(resource.getInputStream(), Job.class);
		} catch (IOException e) {
			log.error("error loading job from yaml file", e);
			throw new RuntimeException(e);
		}
	}
}
