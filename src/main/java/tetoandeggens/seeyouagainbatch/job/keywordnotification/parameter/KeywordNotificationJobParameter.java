package tetoandeggens.seeyouagainbatch.job.keywordnotification.parameter;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@StepScope
@Component
public class KeywordNotificationJobParameter {

	@Value("#{jobParameters['date']}")
	private String date;
}
