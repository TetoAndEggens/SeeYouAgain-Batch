package tetoandeggens.seeyouagainbatch.job.animaldataload.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbatch.job.animaldataload.tasklet.AnimalDataLoadTasklet;
import tetoandeggens.seeyouagainbatch.job.animaldataload.validator.AnimalDataLoadJobParametersValidator;

@Configuration
@RequiredArgsConstructor
public class AnimalDataLoadJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager businessTransactionManager;
    private final AnimalDataLoadTasklet animalDataLoadTasklet;
    private final AnimalDataLoadJobParametersValidator jobParametersValidator;

    @Bean
    public Job animalDataLoadJob() {
        return new JobBuilder("animalDataLoadJob", jobRepository)
                .start(animalDataLoadStep())
                .validator(jobParametersValidator)
                .build();
    }

    @Bean
    public Step animalDataLoadStep() {
        return new StepBuilder("animalDataLoadStep", jobRepository)
                .tasklet(animalDataLoadTasklet, businessTransactionManager)
                .build();
    }
}