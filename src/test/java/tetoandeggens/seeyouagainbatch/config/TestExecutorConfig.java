package tetoandeggens.seeyouagainbatch.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestExecutorConfig {

	@Bean
	@Primary
	public ExecutorService s3UploadExecutorService() {
		return Executors.newFixedThreadPool(2);
	}
}