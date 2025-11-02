package tetoandeggens.seeyouagainbatch.job.s3profileupload.parameter;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@StepScope
@Component
public class S3ProfileUploadJobParameter {

	@Value("#{jobParameters['startDate']}")
	private String startDate;

	@Value("#{jobParameters['endDate']}")
	private String endDate;
}