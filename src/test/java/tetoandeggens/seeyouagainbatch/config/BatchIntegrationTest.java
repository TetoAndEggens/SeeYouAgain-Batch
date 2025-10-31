package tetoandeggens.seeyouagainbatch.config;

import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
public @interface BatchIntegrationTest {
}