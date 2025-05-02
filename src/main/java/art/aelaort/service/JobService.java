package art.aelaort.service;

import art.aelaort.dto.processing.JobData;
import art.aelaort.exceptions.KubernetesJobFailedException;
import art.aelaort.exceptions.KubernetesJobWaitingTimeoutException;
import art.aelaort.utils.TelegramUtils;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.utils.Serialization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "run.mode", havingValue = "bot")
public class JobService {
	private final ResourceLoader resourceLoader;

	public void runJob(URI uri, Message message, String text, boolean isDeleteSourceMessage) {
		try (KubernetesClient client = new KubernetesClientBuilder().build()) {
			Job newJob = parseJob("job.yaml");

			JobData jobData = new JobData(uri, message, text, isDeleteSourceMessage);
			String jobDataStr = TelegramUtils.serializeJobData(jobData);

			List<EnvVar> env = newJob.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
			env.add(envVar("run_mode", "job"));
			env.add(envVar("job_data", jobDataStr));
			newJob.getMetadata().setName(newJob.getMetadata().getName() + randomString());

			Job createdJob = client.batch().v1().jobs().resource(newJob).create();

			waitForJobCompletion(client, createdJob, 30, TimeUnit.MINUTES);
		} catch (KubernetesJobFailedException e) {
			log.error("Job failed, uri {}", uri);
		} catch (KubernetesClientException e) {
			log.error("Job creation or execution failed", e);
		}
	}

	private void waitForJobCompletion(KubernetesClient client, Job job, long timeout, TimeUnit unit) {
		long startTime = System.currentTimeMillis();
		long maxWaitMillis = unit.toMillis(timeout);

		while (System.currentTimeMillis() - startTime < maxWaitMillis) {
			Job runningJob = client.batch().v1().jobs()
					.inNamespace(job.getMetadata().getNamespace())
					.withName(job.getMetadata().getName())
					.get();

			if (isJobCompleted(runningJob)) {
				if (isJobSuccessful(runningJob)) {
					return;
				} else {
					throw new KubernetesJobFailedException();
				}
			}

			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		throw new KubernetesJobWaitingTimeoutException();
	}

	private boolean isJobCompleted(Job job) {
		return job != null
			   && job.getStatus() != null
			   && (job.getStatus().getSucceeded() != null
				   || job.getStatus().getFailed() != null);
	}

	private boolean isJobSuccessful(Job job) {
		return job != null
			   && job.getStatus() != null
			   && job.getStatus().getSucceeded() != null
			   && job.getStatus().getSucceeded() >= 1;
	}

	private String randomString() {
		return UUID.randomUUID().toString().substring(0, 4) + Integer.toHexString((int) System.nanoTime());
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
