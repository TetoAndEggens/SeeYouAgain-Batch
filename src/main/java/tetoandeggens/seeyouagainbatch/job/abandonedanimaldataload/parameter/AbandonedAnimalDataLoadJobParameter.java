package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.parameter;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@StepScope
@Component
public class AbandonedAnimalDataLoadJobParameter {

	@Value("#{jobParameters['startDate']}")
	private String startDate;

	@Value("#{jobParameters['endDate']}")
	private String endDate;

	@Value("#{jobParameters['numOfRows']}")
	private Integer numOfRows;

	@Value("#{jobParameters['pageNo']}")
	private Integer pageNo;
}