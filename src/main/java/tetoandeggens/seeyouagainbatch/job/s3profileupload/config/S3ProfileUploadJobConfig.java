package tetoandeggens.seeyouagainbatch.job.s3profileupload.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManagerFactory;
import tetoandeggens.seeyouagainbatch.common.reader.QuerydslNoOffsetPagingItemReader;
import tetoandeggens.seeyouagainbatch.common.reader.QuerydslNoOffsetPagingItemReaderBuilder;
import tetoandeggens.seeyouagainbatch.common.reader.expression.Expression;
import tetoandeggens.seeyouagainbatch.common.reader.options.QuerydslNoOffsetNumberOptions;
import tetoandeggens.seeyouagainbatch.domain.AnimalProfile;
import tetoandeggens.seeyouagainbatch.domain.QAnimalProfile;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.dto.ProfileImageData;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.parameter.S3ProfileUploadJobParameter;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.processor.S3ProfileUploadProcessor;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.validator.S3ProfileUploadJobParametersValidator;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.writer.S3ProfileUploadWriter;

@Configuration
public class S3ProfileUploadJobConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager businessTransactionManager;
	private final EntityManagerFactory entityManagerFactory;
	private final S3ProfileUploadProcessor s3ProfileUploadProcessor;
	private final S3ProfileUploadWriter s3ProfileUploadWriter;
	private final S3ProfileUploadJobParametersValidator jobParametersValidator;
	private final S3ProfileUploadJobParameter jobParameter;
	private final TaskExecutor s3UploadTaskExecutor;

	public S3ProfileUploadJobConfig(
		JobRepository jobRepository,
		PlatformTransactionManager businessTransactionManager,
		EntityManagerFactory entityManagerFactory,
		S3ProfileUploadProcessor s3ProfileUploadProcessor,
		S3ProfileUploadWriter s3ProfileUploadWriter,
		S3ProfileUploadJobParametersValidator jobParametersValidator,
		S3ProfileUploadJobParameter jobParameter,
		@Autowired(required = false) TaskExecutor s3UploadTaskExecutor
	) {
		this.jobRepository = jobRepository;
		this.businessTransactionManager = businessTransactionManager;
		this.entityManagerFactory = entityManagerFactory;
		this.s3ProfileUploadProcessor = s3ProfileUploadProcessor;
		this.s3ProfileUploadWriter = s3ProfileUploadWriter;
		this.jobParametersValidator = jobParametersValidator;
		this.jobParameter = jobParameter;
		this.s3UploadTaskExecutor = s3UploadTaskExecutor;
	}

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	@Bean
	public Job s3ProfileUploadJob(Step s3ProfileUploadStep) {
		return new JobBuilder("s3ProfileUploadJob", jobRepository)
			.start(s3ProfileUploadStep)
			.validator(jobParametersValidator)
			.build();
	}

	@Bean
	@JobScope
	public Step s3ProfileUploadStep(
		@Value("#{jobParameters['uploadChunkSize'] ?: 500L}") Long uploadChunkSize,
		QuerydslNoOffsetPagingItemReader<AnimalProfile> animalProfileReader
	) {
		var stepBuilder = new StepBuilder("s3ProfileUploadStep", jobRepository)
			.<AnimalProfile, ProfileImageData>chunk(uploadChunkSize.intValue(), businessTransactionManager)
			.reader(animalProfileReader)
			.processor(s3ProfileUploadProcessor)
			.writer(s3ProfileUploadWriter);

		if (s3UploadTaskExecutor != null) {
			stepBuilder.taskExecutor(s3UploadTaskExecutor);
		}

		return stepBuilder.build();
	}

	@Bean
	@StepScope
	public QuerydslNoOffsetPagingItemReader<AnimalProfile> animalProfileReader(
		@Value("#{jobParameters['uploadChunkSize'] ?: 500L}") Long uploadChunkSize) {
		QAnimalProfile profile = QAnimalProfile.animalProfile;

		LocalDate startDate = LocalDate.parse(jobParameter.getStartDate(), DATE_FORMATTER);
		LocalDate endDate = LocalDate.parse(jobParameter.getEndDate(), DATE_FORMATTER);

		QuerydslNoOffsetNumberOptions<AnimalProfile, Long> options =
			QuerydslNoOffsetNumberOptions.of(profile.id, Expression.ASC);

		return QuerydslNoOffsetPagingItemReaderBuilder.<AnimalProfile>builder()
			.entityManagerFactory(entityManagerFactory)
			.pageSize(uploadChunkSize.intValue())
			.options(options)
			.queryFunction(queryFactory -> queryFactory
				.selectFrom(profile)
				.where(profile.happenDate.between(startDate, endDate))
			)
			.build();
	}
}