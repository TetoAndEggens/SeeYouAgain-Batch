package tetoandeggens.seeyouagainbatch.job.fcmtokendelete.integration;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import tetoandeggens.seeyouagainbatch.config.BatchIntegrationTest;
import tetoandeggens.seeyouagainbatch.config.BatchTestConfig;

@BatchIntegrationTest
class FcmTokenDeleteIntegrationTest extends BatchTestConfig {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;

	@Autowired
	private Job fcmTokenDeleteJob;

	private JobParameters jobParameters;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	@BeforeEach
	void setUp() {
		setupTestData();
		setupJobLauncherTestUtils();
		setupJobParameters();
	}

	@AfterEach
	void tearDown() {
		cleanupTestData();
		cleanupJobExecutions();
	}

	@Test
	@DisplayName("FCM 토큰 삭제 Job이 정상적으로 실행되어야 한다")
	void shouldCompleteJobSuccessfully() throws Exception {
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(countAllFcmTokens()).isEqualTo(1);
	}

	@Test
	@DisplayName("cutoff 날짜 이후의 토큰은 삭제하지 않아야 한다")
	void shouldNotDeleteTokensAfterCutoffDate() throws Exception {
		LocalDateTime recentDate = LocalDateTime.now().minusDays(5);
		insertFcmToken(4L, "token-4", "device-4", recentDate);

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(countAllFcmTokens()).isEqualTo(2);
	}

	@Test
	@DisplayName("삭제할 토큰이 없으면 Job이 정상 완료되어야 한다")
	void shouldCompleteSuccessfullyWithNoTokens() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM fcm_token");

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(countAllFcmTokens()).isEqualTo(0);
	}


	@Test
	@DisplayName("대량의 FCM 토큰도 청크 단위로 정상 처리되어야 한다")
	void shouldProcessLargeVolumeInChunks() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM fcm_token");
		setupLargeTestData(1000);

		String dateString = LocalDate.now().format(DATE_FORMATTER);
		JobParameters largeJobParameters = new JobParametersBuilder()
			.addString("date", dateString)
			.addLong("timestamp", System.currentTimeMillis())
			.toJobParameters();

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(largeJobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(countAllFcmTokens()).isEqualTo(0);
	}

	@Test
	@DisplayName("DATE 기준 60일 이전의 토큰들을 모두 삭제해야 한다")
	void shouldDeleteTokensOlderThan60DaysFromDate() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM fcm_token");

		LocalDate baseDate = LocalDate.now();
		LocalDateTime token1Time = baseDate.minusDays(70).atTime(9, 0, 0);
		LocalDateTime token2Time = baseDate.minusDays(61).atTime(12, 0, 0);
		LocalDateTime token3Time = baseDate.minusDays(59).atTime(18, 0, 0);

		insertFcmToken(1L, "token-1", "device-1", token1Time);
		insertFcmToken(2L, "token-2", "device-2", token2Time);
		insertFcmToken(3L, "token-3", "device-3", token3Time);

		String dateString = baseDate.format(DATE_FORMATTER);
		JobParameters dateJobParameters = new JobParametersBuilder()
			.addString("date", dateString)
			.addLong("timestamp", System.currentTimeMillis())
			.toJobParameters();

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(dateJobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(countAllFcmTokens()).isEqualTo(1);
	}

	private void setupJobLauncherTestUtils() {
		jobLauncherTestUtils.setJob(fcmTokenDeleteJob);
	}

	private void setupJobParameters() {
		String targetDate = LocalDate.now().format(DATE_FORMATTER);

		jobParameters = new JobParametersBuilder()
			.addString("date", targetDate)
			.addLong("timestamp", System.currentTimeMillis())
			.toJobParameters();
	}

	private void setupTestData() {
		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO member (member_id, login_id, password, nick_name, phone_number, uuid, role, violated_count, is_push_enabled, is_deleted, created_at, updated_at) " +
				"VALUES (1, 'testuser', 'password123', '테스트닉', '01012345678', 'test-uuid-001', 'USER', 0, false, false, NOW(), NOW())");

		LocalDateTime oldDate = LocalDateTime.now().minusDays(70);
		LocalDateTime recentDate = LocalDateTime.now().minusDays(30);

		insertFcmToken(1L, "token-1", "device-1", oldDate);
		insertFcmToken(2L, "token-2", "device-2", oldDate);
		insertFcmToken(3L, "token-3", "device-3", recentDate);
	}

	private void insertFcmToken(Long id, String token, String deviceId, LocalDateTime lastUsedAt) {
		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			String.format(
				"INSERT INTO fcm_token (fcm_token_id, token, device_id, device_type, last_used_at, member_id, created_at, updated_at) " +
					"VALUES (%d, '%s', '%s', 'AOS', '%s', 1, NOW(), NOW())",
				id, token, deviceId, lastUsedAt.toString()
			)
		);
	}

	private void setupLargeTestData(int count) {
		LocalDateTime targetDate = LocalDateTime.now().minusDays(70);

		StringBuilder sql = new StringBuilder(
			"INSERT INTO fcm_token (fcm_token_id, token, device_id, device_type, last_used_at, member_id, created_at, updated_at) VALUES ");

		for (int i = 1; i <= count; i++) {
			if (i > 1) {
				sql.append(",");
			}
			sql.append(String.format("(%d, 'token-%d', 'device-%d', 'AOS', '%s', 1, NOW(), NOW())",
				i, i, i, targetDate.toString()));
		}

		namedParameterJdbcTemplate.getJdbcTemplate().execute(sql.toString());
	}

	private void cleanupJobExecutions() {
		jobRepositoryTestUtils.removeJobExecutions();
	}

	@Override
	protected void cleanupTestData() {
		namedParameterJdbcTemplate.getJdbcTemplate().execute("SET FOREIGN_KEY_CHECKS = 0");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM fcm_token");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM member");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal_s3_profile");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal_by_keyword");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal_profile");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal_location");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM breed_type");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("SET FOREIGN_KEY_CHECKS = 1");
	}

	private int countAllFcmTokens() {
		return namedParameterJdbcTemplate.getJdbcTemplate()
			.queryForObject("SELECT COUNT(*) FROM fcm_token", Integer.class);
	}
}
