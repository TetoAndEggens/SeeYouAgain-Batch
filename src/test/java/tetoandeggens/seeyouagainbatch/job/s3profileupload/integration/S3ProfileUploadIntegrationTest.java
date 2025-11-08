package tetoandeggens.seeyouagainbatch.job.s3profileupload.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import tetoandeggens.seeyouagainbatch.config.BatchIntegrationTest;
import tetoandeggens.seeyouagainbatch.config.BatchTestConfig;

@BatchIntegrationTest
class S3ProfileUploadIntegrationTest extends BatchTestConfig {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;

	@Autowired
	private Job s3ProfileUploadJob;

	@MockitoBean
	private S3Client s3Client;

	@MockitoBean
	private HttpClient httpClient;

	private JobParameters jobParameters;

	@BeforeEach
	void setUp() {
		setupTestData();
		setupJobLauncherTestUtils();
		setupMockS3Service();
		setupJobParameters();
	}

	@AfterEach
	void tearDown() {
		cleanupTestData();
		cleanupJobExecutions();
	}

	@Test
	@DisplayName("S3 프로필 업로드 Job이 정상적으로 실행되어야 한다")
	void shouldCompleteJobSuccessfully() throws Exception {
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(countS3Profiles()).isEqualTo(3);

		verify(httpClient, times(3)).send(any(), any());
		verify(s3Client, times(3)).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
	}

	@Test
	@DisplayName("날짜 범위에 맞는 프로필만 처리해야 한다")
	void shouldProcessOnlyProfilesWithinDateRange() throws Exception {
		JobParameters dateRangeParameters = jobLauncherTestUtils.getUniqueJobParametersBuilder()
			.addString("startDate", "20250101")
			.addString("endDate", "20250101")
			.toJobParameters();

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(dateRangeParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		assertThat(countS3Profiles()).isEqualTo(1);
		verify(httpClient, times(1)).send(any(), any());
		verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
	}

	@Test
	@DisplayName("프로필이 없으면 Job이 정상 완료되어야 한다")
	void shouldCompleteSuccessfullyWithNoProfiles() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM abandoned_animal_profile");

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(countS3Profiles()).isEqualTo(0);
		verify(httpClient, never()).send(any(), any());
		verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	@DisplayName("S3 업로드 실패 시 해당 항목은 건너뛰고 계속 진행해야 한다")
	void shouldSkipFailedItemsAndContinue() throws Exception {
		HttpResponse<InputStream> successResponse = (HttpResponse<InputStream>) mock(HttpResponse.class);
		HttpHeaders successHeaders = mock(HttpHeaders.class);
		when(successResponse.statusCode()).thenReturn(200);
		when(successResponse.headers()).thenReturn(successHeaders);
		when(successHeaders.firstValueAsLong("Content-Length")).thenReturn(java.util.OptionalLong.of(1024));
		when(successResponse.body()).thenAnswer(invocation -> new ByteArrayInputStream(new byte[1024]));

		when(httpClient.send(any(), any()))
			.thenAnswer(invocation -> successResponse)
			.thenThrow(new RuntimeException("HTTP 요청 실패"))
			.thenAnswer(invocation -> successResponse);

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		assertThat(countS3Profiles()).isEqualTo(2);
		verify(httpClient, times(3)).send(any(), any());
		verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
	}

	@Test
	@DisplayName("대량의 프로필도 청크 단위로 정상 처리되어야 한다")
	void shouldProcessLargeVolumeInChunks() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM abandoned_animal_profile");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM abandoned_animal");

		setupLargeTestData(500);

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(countS3Profiles()).isEqualTo(500);

		verify(httpClient, times(500)).send(any(), any());
		verify(s3Client, times(500)).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
	}

	private void setupJobLauncherTestUtils() {
		jobLauncherTestUtils.setJob(s3ProfileUploadJob);
	}

	@SuppressWarnings("unchecked")
	private void setupMockS3Service() {
		try {
			HttpResponse<InputStream> response = (HttpResponse<InputStream>) mock(HttpResponse.class);
			HttpHeaders headers = mock(HttpHeaders.class);

			when(response.statusCode()).thenReturn(200);
			when(response.headers()).thenReturn(headers);
			when(headers.firstValueAsLong("Content-Length")).thenReturn(java.util.OptionalLong.of(1024));
			when(response.body()).thenAnswer(invocation -> new ByteArrayInputStream(new byte[1024]));

			when(httpClient.send(any(), any())).thenAnswer(invocation -> response);
			when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
				.thenReturn(PutObjectResponse.builder().build());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void setupJobParameters() {
		jobParameters = jobLauncherTestUtils.getUniqueJobParametersBuilder()
			.addString("startDate", "20250101")
			.addString("endDate", "20250103")
			.toJobParameters();
	}

	private void setupLargeTestData(int count) {
		insertLargeTestAbandonedAnimals(count);
		insertLargeTestProfiles(count);
	}

	private void insertLargeTestAbandonedAnimals(int count) {
		StringBuilder sql = new StringBuilder(
			"INSERT INTO abandoned_animal (abandoned_animal_id, desertion_no, happen_date, happen_place, " +
				"color, birth, weight, notice_no, notice_start_date, notice_end_date, " +
				"process_state, sex, neutered_state, special_mark, species, center_location_id, breed_type_id, created_at, updated_at) VALUES ");

		for (int i = 1; i <= count; i++) {
			if (i > 1) {
				sql.append(",");
			}
			sql.append(String.format("(%d, 'TEST-%06d', '2025-01-01', '서울시', '갈색', '2020(년생)', '5(Kg)', " +
				"'NOTICE-%06d', '2025-01-01', '2025-01-15', " +
				"'보호중', 'M', 'Y', '테스트', 'DOG', 1, 1, NOW(), NOW())", i, i, i));
		}

		namedParameterJdbcTemplate.getJdbcTemplate().execute(sql.toString());
	}

	private void insertLargeTestProfiles(int count) {
		StringBuilder sql = new StringBuilder(
			"INSERT INTO abandoned_animal_profile (abandoned_animal_profile_id, profile, happen_date, abandoned_animal_id, created_at, updated_at) VALUES ");

		for (int i = 1; i <= count; i++) {
			if (i > 1) {
				sql.append(",");
			}
			sql.append(String.format("(%d, 'http://example.com/profile-%d.jpg', '2025-01-01', %d, '2025-01-01 10:00:00', '2025-01-01 10:00:00')", i, i, i));
		}

		namedParameterJdbcTemplate.getJdbcTemplate().execute(sql.toString());
	}

	private void cleanupJobExecutions() {
		jobRepositoryTestUtils.removeJobExecutions();
	}

	private void setupTestData() {
		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO center_location (center_location_id, name, address, center_no, coordinates, created_at, updated_at) " +
				"VALUES (1, '테스트 보호소', '서울시 강남구', 'TEST-001', ST_GeomFromText('POINT(37.0 127.0)', 4326), NOW(), NOW())");

		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO breed_type (breed_type_id, code, name, type, created_at, updated_at) " +
				"VALUES (1, '417000', '믹스견', 'DOG', NOW(), NOW())");

		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO abandoned_animal (abandoned_animal_id, desertion_no, happen_date, happen_place, " +
				"color, birth, weight, notice_no, notice_start_date, notice_end_date, " +
				"process_state, sex, neutered_state, special_mark, species, " +
				"center_location_id, breed_type_id, created_at, updated_at) VALUES " +
				"(1, 'TEST-001', '2025-01-01', '서울시 강남구', '갈색', '2020(년생)', '5(Kg)', 'NOTICE-001', " +
				"'2025-01-01', '2025-01-15', '보호중', 'M', 'Y', '순함', 'DOG', 1, 1, NOW(), NOW()), " +
				"(2, 'TEST-002', '2025-01-02', '서울시 송파구', '흰색', '2021(년생)', '4(Kg)', 'NOTICE-002', " +
				"'2025-01-02', '2025-01-16', '보호중', 'F', 'N', '활발', 'DOG', 1, 1, NOW(), NOW()), " +
				"(3, 'TEST-003', '2025-01-03', '서울시 관악구', '검은색', '2022(년생)', '3(Kg)', 'NOTICE-003', " +
				"'2025-01-03', '2025-01-17', '보호중', 'M', 'Y', '조용함', 'DOG', 1, 1, NOW(), NOW())");

		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO abandoned_animal_profile (abandoned_animal_profile_id, profile, happen_date, abandoned_animal_id, created_at, updated_at) VALUES " +
				"(1, 'http://example.com/profile1.jpg', '2025-01-01', 1, '2025-01-01 10:00:00', '2025-01-01 10:00:00'), " +
				"(2, 'http://example.com/profile2.jpg', '2025-01-02', 2, '2025-01-02 10:00:00', '2025-01-02 10:00:00'), " +
				"(3, 'http://example.com/profile3.jpg', '2025-01-03', 3, '2025-01-03 10:00:00', '2025-01-03 10:00:00')");
	}

	private int countS3Profiles() {
		return namedParameterJdbcTemplate.getJdbcTemplate()
			.queryForObject("SELECT COUNT(*) FROM abandoned_animal_s3_profile", Integer.class);
	}
}