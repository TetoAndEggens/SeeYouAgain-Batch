package tetoandeggens.seeyouagainbatch.job.s3profiledelete.config;

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

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbatch.common.reader.QuerydslNoOffsetPagingItemReader;
import tetoandeggens.seeyouagainbatch.common.reader.QuerydslNoOffsetPagingItemReaderBuilder;
import tetoandeggens.seeyouagainbatch.common.reader.expression.Expression;
import tetoandeggens.seeyouagainbatch.common.reader.options.QuerydslNoOffsetNumberOptions;
import tetoandeggens.seeyouagainbatch.domain.AnimalS3Profile;
import tetoandeggens.seeyouagainbatch.domain.QAnimalS3Profile;
import tetoandeggens.seeyouagainbatch.job.s3profiledelete.processor.S3ProfileDeleteProcessor;
import tetoandeggens.seeyouagainbatch.job.s3profiledelete.validator.S3ProfileDeleteJobParametersValidator;
import tetoandeggens.seeyouagainbatch.job.s3profiledelete.writer.S3ProfileDeleteWriter;

@Configuration
@RequiredArgsConstructor
public class S3ProfileDeleteJobConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager businessTransactionManager;
	private final EntityManagerFactory entityManagerFactory;
	private final S3ProfileDeleteProcessor s3ProfileDeleteProcessor;
	private final S3ProfileDeleteWriter s3ProfileDeleteWriter;
	private final S3ProfileDeleteJobParametersValidator jobParametersValidator;

	@Bean
	public Job s3ProfileDeleteJob(Step s3ProfileDeleteStep) {
		return new JobBuilder("s3ProfileDeleteJob", jobRepository)
			.start(s3ProfileDeleteStep)
			.validator(jobParametersValidator)
			.build();
	}

	@Bean
	@JobScope
	public Step s3ProfileDeleteStep(
		@Value("#{jobParameters['deleteChunkSize'] ?: 500L}") Long deleteChunkSize,
		QuerydslNoOffsetPagingItemReader<AnimalS3Profile> animalS3ProfileReader
	) {
		return new StepBuilder("s3ProfileDeleteStep", jobRepository)
			.<AnimalS3Profile, AnimalS3Profile>chunk(deleteChunkSize.intValue(), businessTransactionManager)
			.reader(animalS3ProfileReader)
			.processor(s3ProfileDeleteProcessor)
			.writer(s3ProfileDeleteWriter)
			.build();
	}

	@Bean
	@StepScope
	public QuerydslNoOffsetPagingItemReader<AnimalS3Profile> animalS3ProfileReader(
		@Value("#{jobParameters['deleteChunkSize'] ?: 500L}") Long deleteChunkSize) {
		QAnimalS3Profile s3Profile = QAnimalS3Profile.animalS3Profile;

		QuerydslNoOffsetNumberOptions<AnimalS3Profile, Long> options =
			QuerydslNoOffsetNumberOptions.of(s3Profile.id, Expression.ASC);

		return QuerydslNoOffsetPagingItemReaderBuilder.<AnimalS3Profile>builder()
			.entityManagerFactory(entityManagerFactory)
			.pageSize(deleteChunkSize.intValue())
			.options(options)
			.queryFunction(queryFactory -> queryFactory
				.selectFrom(s3Profile)
				.where(s3Profile.isDeleted.eq(true))
			)
			.build();
	}
}