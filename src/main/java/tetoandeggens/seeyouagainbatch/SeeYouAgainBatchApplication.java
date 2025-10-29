package tetoandeggens.seeyouagainbatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class SeeYouAgainBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeeYouAgainBatchApplication.class, args);
	}
}