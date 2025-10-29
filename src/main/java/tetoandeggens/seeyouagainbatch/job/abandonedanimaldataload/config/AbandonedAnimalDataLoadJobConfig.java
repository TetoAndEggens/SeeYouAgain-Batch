package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.tasklet.AbandonedAnimalDataLoadTasklet;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.validator.AbandonedAnimalDataLoadJobParametersValidator;

@Configuration
@RequiredArgsConstructor
public class AbandonedAnimalDataLoadJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager businessTransactionManager;
    private final AbandonedAnimalDataLoadTasklet abandonedAnimalDataLoadTasklet;
    private final AbandonedAnimalDataLoadJobParametersValidator jobParametersValidator;

    @Bean
    public Job abandonedAnimalDataLoadJob() {
        return new JobBuilder("abandonedAnimalDataLoadJob", jobRepository)
                .start(abandonedAnimalDataLoadStep())
                .validator(jobParametersValidator)
                .build();
    }

    @Bean
    public Step abandonedAnimalDataLoadStep() {
        return new StepBuilder("abandonedAnimalDataLoadStep", jobRepository)
                .tasklet(abandonedAnimalDataLoadTasklet, businessTransactionManager)
                .build();
    }
}
