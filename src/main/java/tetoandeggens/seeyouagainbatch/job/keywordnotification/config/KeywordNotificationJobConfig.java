package tetoandeggens.seeyouagainbatch.job.keywordnotification.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.transaction.PlatformTransactionManager;

import com.querydsl.core.types.Projections;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbatch.common.reader.QuerydslPagingItemReader;
import tetoandeggens.seeyouagainbatch.domain.QAnimalByKeyword;
import tetoandeggens.seeyouagainbatch.domain.QNotificationKeyword;
import tetoandeggens.seeyouagainbatch.job.keywordnotification.dto.KeywordNotificationDto;
import tetoandeggens.seeyouagainbatch.job.keywordnotification.parameter.KeywordNotificationJobParameter;
import tetoandeggens.seeyouagainbatch.job.keywordnotification.validator.KeywordNotificationJobParametersValidator;
import tetoandeggens.seeyouagainbatch.job.keywordnotification.writer.KeywordNotificationWriter;

@Configuration
@RequiredArgsConstructor
public class KeywordNotificationJobConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager businessTransactionManager;
	private final EntityManagerFactory entityManagerFactory;
	private final KeywordNotificationWriter keywordNotificationWriter;
	private final KeywordNotificationJobParametersValidator keywordNotificationJobParametersValidator;
	private final KeywordNotificationJobParameter keywordNotificationJobParameter;

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	@Bean
	public Job keywordNotificationJob(Step keywordNotificationStep) {
		return new JobBuilder("keywordNotificationJob", jobRepository)
			.validator(keywordNotificationJobParametersValidator)
			.start(keywordNotificationStep)
			.build();
	}

	@Bean
	@JobScope
	public Step keywordNotificationStep(
		@Value("#{jobParameters['notificationChunkSize'] ?: 500L}") Long notificationChunkSize,
		QuerydslPagingItemReader<KeywordNotificationDto> keywordNotificationReader
	) {
		return new StepBuilder("keywordNotificationStep", jobRepository)
			.<KeywordNotificationDto, KeywordNotificationDto>chunk(notificationChunkSize.intValue(), businessTransactionManager)
			.reader(keywordNotificationReader)
			.writer(keywordNotificationWriter)
			.build();
	}

	@Bean
	@StepScope
	public QuerydslPagingItemReader<KeywordNotificationDto> keywordNotificationReader(
		@Value("#{jobParameters['notificationChunkSize'] ?: 500L}") Long notificationChunkSize) {
		QAnimalByKeyword abk = QAnimalByKeyword.animalByKeyword;
		QNotificationKeyword nk = QNotificationKeyword.notificationKeyword;

		LocalDate targetDate = LocalDate.parse(keywordNotificationJobParameter.getDate(), DATE_FORMATTER);
		LocalDateTime startOfDay = targetDate.atStartOfDay();
		LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

		return new QuerydslPagingItemReader<>(
			entityManagerFactory,
			notificationChunkSize.intValue(),
			queryFactory -> queryFactory
				.select(Projections.constructor(KeywordNotificationDto.class,
					abk.notificationKeyword.id,
					nk.keyword,
					abk.count(),
					nk.keywordType,
					nk.keywordCategoryType
				))
				.from(abk)
				.join(abk.notificationKeyword, nk)
				.where(
					abk.createdAt.goe(startOfDay),
					abk.createdAt.lt(endOfDay)
				)
				.groupBy(abk.notificationKeyword.id)
				.orderBy(abk.notificationKeyword.id.asc())
		);
	}
}