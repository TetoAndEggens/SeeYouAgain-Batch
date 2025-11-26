package tetoandeggens.seeyouagainbatch.job.fcmtokendelete.config;

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
import tetoandeggens.seeyouagainbatch.domain.FcmToken;
import tetoandeggens.seeyouagainbatch.domain.QFcmToken;
import tetoandeggens.seeyouagainbatch.job.fcmtokendelete.parameter.FcmTokenDeleteJobParameter;
import tetoandeggens.seeyouagainbatch.job.fcmtokendelete.validator.FcmTokenDeleteJobParametersValidator;
import tetoandeggens.seeyouagainbatch.job.fcmtokendelete.writer.FcmTokenDeleteWriter;

@Configuration
@RequiredArgsConstructor
public class FcmTokenDeleteJobConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager businessTransactionManager;
	private final EntityManagerFactory entityManagerFactory;
	private final FcmTokenDeleteWriter fcmTokenDeleteWriter;
	private final FcmTokenDeleteJobParametersValidator fcmTokenDeleteJobParametersValidator;
	private final FcmTokenDeleteJobParameter fcmTokenDeleteJobParameter;

	private static final int CHUNK_SIZE = 500;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	@Bean
	public Job fcmTokenDeleteJob() {
		return new JobBuilder("fcmTokenDeleteJob", jobRepository)
			.validator(fcmTokenDeleteJobParametersValidator)
			.start(fcmTokenDeleteStep())
			.build();
	}

	@Bean
	public Step fcmTokenDeleteStep() {
		return new StepBuilder("fcmTokenDeleteStep", jobRepository)
			.<FcmToken, FcmToken>chunk(CHUNK_SIZE, businessTransactionManager)
			.reader(fcmTokenReader())
			.writer(fcmTokenDeleteWriter)
			.build();
	}

	@Bean
	@StepScope
	public QuerydslNoOffsetPagingItemReader<FcmToken> fcmTokenReader() {
		QFcmToken fcmToken = QFcmToken.fcmToken;

		LocalDate date = LocalDate.parse(fcmTokenDeleteJobParameter.getDate(), DATE_FORMATTER);
		LocalDateTime cutoffDate = date.minusDays(60).atStartOfDay();

		QuerydslNoOffsetNumberOptions<FcmToken, Long> options =
			QuerydslNoOffsetNumberOptions.of(fcmToken.id, Expression.ASC);

		return QuerydslNoOffsetPagingItemReaderBuilder.<FcmToken>builder()
			.entityManagerFactory(entityManagerFactory)
			.pageSize(CHUNK_SIZE)
			.options(options)
			.queryFunction(queryFactory -> queryFactory
				.selectFrom(fcmToken)
				.where(fcmToken.lastUsedAt.lt(cutoffDate))
			)
			.build();
	}
}