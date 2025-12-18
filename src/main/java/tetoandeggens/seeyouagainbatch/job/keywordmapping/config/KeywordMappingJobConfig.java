package tetoandeggens.seeyouagainbatch.job.keywordmapping.config;

import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.querydsl.core.types.Projections;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbatch.common.reader.QuerydslPagingItemReader;
import tetoandeggens.seeyouagainbatch.domain.AnimalByKeyword;
import tetoandeggens.seeyouagainbatch.domain.QNotificationKeyword;
import tetoandeggens.seeyouagainbatch.job.keywordmapping.dto.KeywordCombinationDto;
import tetoandeggens.seeyouagainbatch.job.keywordmapping.processor.KeywordMappingProcessor;
import tetoandeggens.seeyouagainbatch.job.keywordmapping.validator.KeywordMappingJobParametersValidator;
import tetoandeggens.seeyouagainbatch.job.keywordmapping.writer.KeywordMappingWriter;

@Configuration
@RequiredArgsConstructor
public class KeywordMappingJobConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager businessTransactionManager;
	private final EntityManagerFactory entityManagerFactory;
	private final KeywordMappingWriter keywordMappingWriter;
	private final KeywordMappingJobParametersValidator keywordMappingJobParametersValidator;
	private final KeywordMappingProcessor keywordMappingProcessor;

	@Bean
	public Job keywordMappingJob(Step keywordMappingStep) {
		return new JobBuilder("keywordMappingJob", jobRepository)
			.validator(keywordMappingJobParametersValidator)
			.start(keywordMappingStep)
			.build();
	}

	@Bean
	@JobScope
	public Step keywordMappingStep(
		@Value("#{jobParameters['mappingChunkSize'] ?: 500L}") Long mappingChunkSize,
		QuerydslPagingItemReader<KeywordCombinationDto> keywordCombinationReader
	) {
		return new StepBuilder("keywordMappingStep", jobRepository)
			.<KeywordCombinationDto, List<AnimalByKeyword>>chunk(mappingChunkSize.intValue(), businessTransactionManager)
			.reader(keywordCombinationReader)
			.processor(keywordMappingProcessor)
			.writer(keywordMappingWriter)
			.build();
	}

	@Bean
	@StepScope
	public QuerydslPagingItemReader<KeywordCombinationDto> keywordCombinationReader(
		@Value("#{jobParameters['mappingChunkSize'] ?: 500L}") Long mappingChunkSize) {
		QNotificationKeyword nk = QNotificationKeyword.notificationKeyword;

		return new QuerydslPagingItemReader<>(
			entityManagerFactory,
			mappingChunkSize.intValue(),
			queryFactory -> queryFactory
				.select(Projections.constructor(KeywordCombinationDto.class,
					nk.id.min(),
					nk.keyword,
					nk.keywordType,
					nk.keywordCategoryType
				))
				.from(nk)
				.groupBy(nk.keyword, nk.keywordType, nk.keywordCategoryType)
				.orderBy(nk.id.min().asc())
		);
	}
}