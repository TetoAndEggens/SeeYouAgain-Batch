package tetoandeggens.seeyouagainbatch.job.s3profiledelete.parameter;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@StepScope
@Component
public class S3ProfileDeleteJobParameter {

	@Value("#{jobParameters['startDate']}")
	private String startDate;

	@Value("#{jobParameters['endDate']}")
	private String endDate;
}