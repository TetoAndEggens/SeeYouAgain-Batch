package tetoandeggens.seeyouagainbatch.job.elasticsearchsync.parameter;

import lombok.Getter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@StepScope
@Component
public class ElasticsearchSyncJobParameter {

    @Value("#{jobParameters['startDate']}")
    private String startDate;

    @Value("#{jobParameters['endDate']}")
    private String endDate;
}