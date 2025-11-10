package tetoandeggens.seeyouagainbatch.job.elasticsearchsync.config;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import tetoandeggens.seeyouagainbatch.common.reader.QuerydslNoOffsetPagingItemReader;
import tetoandeggens.seeyouagainbatch.common.reader.QuerydslNoOffsetPagingItemReaderBuilder;
import tetoandeggens.seeyouagainbatch.common.reader.expression.Expression;
import tetoandeggens.seeyouagainbatch.common.reader.options.QuerydslNoOffsetNumberOptions;
import tetoandeggens.seeyouagainbatch.domain.AbandonedAnimal;
import tetoandeggens.seeyouagainbatch.domain.QAbandonedAnimal;
import tetoandeggens.seeyouagainbatch.job.elasticsearchsync.parameter.ElasticsearchSyncJobParameter;
import tetoandeggens.seeyouagainbatch.job.elasticsearchsync.validator.ElasticsearchSyncJobParametersValidator;
import tetoandeggens.seeyouagainbatch.job.elasticsearchsync.writer.AbandonedAnimalElasticsearchWriter;

@Configuration
@RequiredArgsConstructor
public class ElasticsearchSyncJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager businessTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final AbandonedAnimalElasticsearchWriter elasticsearchWriter;
    private final ElasticsearchSyncJobParametersValidator jobParametersValidator;
    private final ElasticsearchSyncJobParameter jobParameter;

    private static final int CHUNK_SIZE = 500;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Bean
    public Job elasticsearchSyncJob() {
        return new JobBuilder("elasticsearchSyncJob", jobRepository)
            .start(elasticsearchSyncStep())
            .validator(jobParametersValidator)
            .build();
    }

    @Bean
    public Step elasticsearchSyncStep() {
        return new StepBuilder("elasticsearchSyncStep", jobRepository)
            .<AbandonedAnimal, AbandonedAnimal>chunk(CHUNK_SIZE, businessTransactionManager)
            .reader(abandonedAnimalReader())
            .writer(elasticsearchWriter)
            .build();
    }

    @Bean
    @StepScope
    public QuerydslNoOffsetPagingItemReader<AbandonedAnimal> abandonedAnimalReader() {
        QAbandonedAnimal animal = QAbandonedAnimal.abandonedAnimal;

        LocalDate startDate = LocalDate.parse(jobParameter.getStartDate(), DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(jobParameter.getEndDate(), DATE_FORMATTER);

        QuerydslNoOffsetNumberOptions<AbandonedAnimal, Long> options =
            QuerydslNoOffsetNumberOptions.of(animal.id, Expression.ASC);

        return QuerydslNoOffsetPagingItemReaderBuilder.<AbandonedAnimal>builder()
            .entityManagerFactory(entityManagerFactory)
            .pageSize(CHUNK_SIZE)
            .options(options)
            .queryFunction(queryFactory -> queryFactory
                .selectFrom(animal)
                .leftJoin(animal.breedType)
                .where(animal.happenDate.between(startDate, endDate))
            )
            .build();
    }
}