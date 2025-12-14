package tetoandeggens.seeyouagainbatch.job.s3profileupload.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.config.NamedThreadFactory;

@Slf4j
@Configuration
@Profile("!test")
public class S3UploadExecutorConfig {

	private ThreadPoolExecutor executorService;

	@Bean
	public ExecutorService s3UploadExecutorService(@Value("${batch.s3upload.thread-pool-size}") int threadPoolSize) {
		this.executorService = new ThreadPoolExecutor(
			threadPoolSize,
			threadPoolSize,
			0L,
			TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(),
			new NamedThreadFactory("s3-upload"),
			new ThreadPoolExecutor.CallerRunsPolicy()
		);

		return this.executorService;
	}

	@PreDestroy
	public void destroy() {
		if (executorService != null) {
			log.info("S3 업로드용 ExecutorService 종료 시작");
			executorService.shutdown();
			try {
				if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
					if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
						log.error("S3 업로드용 ExecutorService가 정상적으로 종료되지 않았습니다");
					}
				}
			} catch (InterruptedException e) {
				executorService.shutdownNow();
				Thread.currentThread().interrupt();
			}
			log.info("S3 업로드용 ExecutorService 종료 완료");
		}
	}
}