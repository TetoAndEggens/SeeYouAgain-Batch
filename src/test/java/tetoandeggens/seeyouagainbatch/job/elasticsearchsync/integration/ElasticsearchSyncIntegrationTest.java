package tetoandeggens.seeyouagainbatch.job.elasticsearchsync.integration;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import tetoandeggens.seeyouagainbatch.config.BatchIntegrationTest;
import tetoandeggens.seeyouagainbatch.config.BatchTestConfig;

@BatchIntegrationTest
class ElasticsearchSyncIntegrationTest extends BatchTestConfig {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;

	@Autowired
	private Job elasticsearchSyncJob;

	@MockitoBean
	private ElasticsearchClient elasticsearchClient;

	@MockitoBean
	private ElasticsearchConverter elasticsearchConverter;

	@MockitoBean
	private ElasticsearchOperations elasticsearchOperations;

	private JobParameters jobParameters;

	@BeforeEach
	void setUp() {
		setupTestData();
		setupJobLauncherTestUtils();
		setupMockElasticsearch();
		setupJobParameters();
	}

	@AfterEach
	void tearDown() {
		cleanupTestData();
		cleanupJobExecutions();
	}

	@Test
	@DisplayName("Elasticsearch 동기화 Job이 정상적으로 실행되어야 한다")
	void shouldCompleteJobSuccessfully() throws Exception {
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		verify(elasticsearchClient, times(1)).bulk(any(BulkRequest.class));
	}

	@Test
	@DisplayName("날짜 범위에 맞는 유기동물만 동기화해야 한다")
	void shouldSyncOnlyAnimalsWithinDateRange() throws Exception {
		JobParameters dateRangeParameters = jobLauncherTestUtils.getUniqueJobParametersBuilder()
			.addString("startDate", "20250101")
			.addString("endDate", "20250101")
			.toJobParameters();

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(dateRangeParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		verify(elasticsearchClient, times(1)).bulk(any(BulkRequest.class));
	}

	@Test
	@DisplayName("유기동물이 없으면 Job이 정상 완료되어야 한다")
	void shouldCompleteSuccessfullyWithNoAnimals() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM abandoned_animal");

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		verify(elasticsearchClient, never()).bulk(any(BulkRequest.class));
	}

	@Test
	@DisplayName("품종 정보가 없어도 동기화가 정상적으로 처리되어야 한다")
	void shouldHandleAnimalsWithoutBreedType() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO abandoned_animal (abandoned_animal_id, desertion_no, happen_date, happen_place, " +
				"color, birth, weight, notice_no, notice_start_date, notice_end_date, " +
				"process_state, sex, neutered_state, special_mark, species, center_location_id, breed_type_id, created_at, updated_at) VALUES " +
				"(4, 'TEST-NO-BREED', '2025-01-04', '서울시 중구', '회색', '2023(년생)', '6(Kg)', 'NOTICE-004', " +
				"'2025-01-04', '2025-01-18', '보호중', 'M', 'Y', '조용함', 'DOG', 1, NULL, NOW(), NOW())");

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		verify(elasticsearchClient, times(1)).bulk(any(BulkRequest.class));
	}

	private void setupJobLauncherTestUtils() {
		jobLauncherTestUtils.setJob(elasticsearchSyncJob);
	}

	private void setupMockElasticsearch() {
		try {
			BulkResponse response = mock(BulkResponse.class);
			when(response.errors()).thenReturn(false);
			when(elasticsearchClient.bulk(any(BulkRequest.class))).thenReturn(response);
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
	}

	private void cleanupJobExecutions() {
		jobRepositoryTestUtils.removeJobExecutions();
	}
}