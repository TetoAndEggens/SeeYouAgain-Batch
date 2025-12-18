package tetoandeggens.seeyouagainbatch.job.s3profileupload.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import jakarta.annotation.PreDestroy;
import tetoandeggens.seeyouagainbatch.config.NamedThreadFactory;

@Configuration
@Profile("!test")
public class S3UploadExecutorConfig {

	private ThreadPoolExecutor s3UploadInternalExecutorService;

	@Bean
	public ExecutorService s3UploadExecutorService(@Value("${batch.s3upload.thread-pool-size}") int threadPoolSize) {
		this.s3UploadInternalExecutorService = new ThreadPoolExecutor(
			threadPoolSize,
			threadPoolSize,
			0L,
			TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(),
			new NamedThreadFactory("s3-upload"),
			new ThreadPoolExecutor.CallerRunsPolicy()
		);

		return this.s3UploadInternalExecutorService;
	}

	@Bean
	public TaskExecutor s3UploadTaskExecutor(@Value("${batch.s3upload.thread-pool-size}") int threadPoolSize) {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();

		taskExecutor.setCorePoolSize(threadPoolSize);
		taskExecutor.setMaxPoolSize(threadPoolSize);
		taskExecutor.setQueueCapacity(0);
		taskExecutor.setThreadFactory(new NamedThreadFactory("s3-upload-step"));
		taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		taskExecutor.initialize();

		return taskExecutor;
	}


	@PreDestroy
	public void destroy() {
		if (s3UploadInternalExecutorService != null) {
			s3UploadInternalExecutorService.shutdown();
			try {
				if (!s3UploadInternalExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
					s3UploadInternalExecutorService.shutdownNow();
					if (!s3UploadInternalExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
					}
				}
			} catch (InterruptedException e) {
				s3UploadInternalExecutorService.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}
}