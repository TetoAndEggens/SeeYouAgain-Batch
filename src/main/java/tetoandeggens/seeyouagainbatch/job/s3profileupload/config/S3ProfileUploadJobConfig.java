package tetoandeggens.seeyouagainbatch.job.s3profileupload.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbatch.common.reader.QuerydslNoOffsetPagingItemReader;
import tetoandeggens.seeyouagainbatch.common.reader.QuerydslNoOffsetPagingItemReaderBuilder;
import tetoandeggens.seeyouagainbatch.common.reader.expression.Expression;
import tetoandeggens.seeyouagainbatch.common.reader.options.QuerydslNoOffsetNumberOptions;
import tetoandeggens.seeyouagainbatch.domain.AbandonedAnimalProfile;
import tetoandeggens.seeyouagainbatch.domain.AbandonedAnimalS3Profile;
import tetoandeggens.seeyouagainbatch.domain.QAbandonedAnimalProfile;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.parameter.S3ProfileUploadJobParameter;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.processor.S3ProfileUploadProcessor;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.validator.S3ProfileUploadJobParametersValidator;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.writer.S3ProfileUploadWriter;

@Configuration
@RequiredArgsConstructor
public class S3ProfileUploadJobConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager businessTransactionManager;
	private final EntityManagerFactory entityManagerFactory;
	private final S3ProfileUploadProcessor s3ProfileUploadProcessor;
	private final S3ProfileUploadWriter s3ProfileUploadWriter;
	private final S3ProfileUploadJobParametersValidator jobParametersValidator;
	private final S3ProfileUploadJobParameter jobParameter;

	private static final int CHUNK_SIZE = 500;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	@Bean
	public Job s3ProfileUploadJob() {
		return new JobBuilder("s3ProfileUploadJob", jobRepository)
			.start(s3ProfileUploadStep())
			.validator(jobParametersValidator)
			.build();
	}

	@Bean
	public Step s3ProfileUploadStep() {
		return new StepBuilder("s3ProfileUploadStep", jobRepository)
			.<AbandonedAnimalProfile, AbandonedAnimalS3Profile>chunk(CHUNK_SIZE, businessTransactionManager)
			.reader(abandonedAnimalProfileReader())
			.processor(s3ProfileUploadProcessor)
			.writer(s3ProfileUploadWriter)
			.build();
	}

	@Bean
	@StepScope
	public QuerydslNoOffsetPagingItemReader<AbandonedAnimalProfile> abandonedAnimalProfileReader() {
		QAbandonedAnimalProfile profile = QAbandonedAnimalProfile.abandonedAnimalProfile;

		LocalDate startDate = LocalDate.parse(jobParameter.getStartDate(), DATE_FORMATTER);
		LocalDate endDate = LocalDate.parse(jobParameter.getEndDate(), DATE_FORMATTER);
		LocalDateTime startDateTime = startDate.atStartOfDay();
		LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

		QuerydslNoOffsetNumberOptions<AbandonedAnimalProfile, Long> options =
			QuerydslNoOffsetNumberOptions.of(profile.id, Expression.ASC);

		return QuerydslNoOffsetPagingItemReaderBuilder.<AbandonedAnimalProfile>builder()
			.entityManagerFactory(entityManagerFactory)
			.pageSize(CHUNK_SIZE)
			.options(options)
			.queryFunction(queryFactory -> queryFactory
				.selectFrom(profile)
				.where(profile.createdAt.between(startDateTime, endDateTime))
			)
			.build();
	}
}