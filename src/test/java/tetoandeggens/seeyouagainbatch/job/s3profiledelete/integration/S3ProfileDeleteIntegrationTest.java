package tetoandeggens.seeyouagainbatch.job.s3profiledelete.integration;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import tetoandeggens.seeyouagainbatch.config.BatchIntegrationTest;
import tetoandeggens.seeyouagainbatch.config.BatchTestConfig;

@BatchIntegrationTest
class S3ProfileDeleteIntegrationTest extends BatchTestConfig {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;

	@Autowired
	private Job s3ProfileDeleteJob;

	@MockitoBean
	private S3Client s3Client;

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
	@DisplayName("S3 프로필 삭제 Job이 정상적으로 실행되어야 한다")
	void shouldCompleteJobSuccessfully() throws Exception {
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(countDeletedS3Profiles()).isEqualTo(0);

		verify(s3Client, times(3)).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("isDeleted가 false인 프로필은 처리하지 않아야 한다")
	void shouldNotProcessNonDeletedProfiles() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO animal_s3_profile (animal_s3_profile_id, profile, image_type, is_deleted, animal_id, created_at, updated_at) " +
				"VALUES (4, 'animal-profiles/test4.webp', 'WEBP', false, 1, NOW(), NOW())");

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		assertThat(countAllS3Profiles()).isEqualTo(1);
		verify(s3Client, times(3)).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("삭제할 프로필이 없으면 Job이 정상 완료되어야 한다")
	void shouldCompleteSuccessfullyWithNoProfiles() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal_s3_profile");

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(countAllS3Profiles()).isEqualTo(0);
		verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("S3에 파일이 없어도(404) Job은 정상 완료되어야 한다")
	void shouldCompleteSuccessfullyWhenS3FileNotFound() throws Exception {
		S3Exception s3Exception = (S3Exception) S3Exception.builder()
			.statusCode(404)
			.message("Not Found")
			.build();

		when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
			.thenThrow(s3Exception);

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		assertThat(countDeletedS3Profiles()).isEqualTo(0);
		verify(s3Client, times(3)).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("S3 삭제 실패 시 해당 항목은 건너뛰고 계속 진행해야 한다")
	void shouldSkipFailedItemsAndContinue() throws Exception {
		S3Exception s3Exception = (S3Exception) S3Exception.builder()
			.statusCode(500)
			.message("Internal Server Error")
			.build();

		when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
			.thenReturn(DeleteObjectResponse.builder().build())
			.thenThrow(s3Exception)
			.thenReturn(DeleteObjectResponse.builder().build());

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		assertThat(countDeletedS3Profiles()).isEqualTo(1);
		assertThat(countAllS3Profiles()).isEqualTo(1);
		verify(s3Client, times(3)).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("대량의 프로필도 청크 단위로 정상 처리되어야 한다")
	void shouldProcessLargeVolumeInChunks() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal_s3_profile");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal_profile");

		setupLargeTestData(1000);

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(countAllS3Profiles()).isEqualTo(0);

		verify(s3Client, times(1000)).deleteObject(any(DeleteObjectRequest.class));
	}

	private void setupJobLauncherTestUtils() {
		jobLauncherTestUtils.setJob(s3ProfileDeleteJob);
	}

	private void setupMockS3Service() {
		when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
			.thenReturn(DeleteObjectResponse.builder().build());
	}

	private void setupJobParameters() {
		jobParameters = jobLauncherTestUtils.getUniqueJobParametersBuilder()
			.addString("startDate", "20250101")
			.addString("endDate", "20251231")
			.toJobParameters();
	}

	private void setupLargeTestData(int count) {
		insertLargeTestAnimals(count);
		insertLargeTestS3Profiles(count);
	}

	private void insertLargeTestAnimals(int count) {
		StringBuilder sql = new StringBuilder(
			"INSERT INTO animal (animal_id, desertion_no, happen_date, happen_place, " +
				"color, birth, weight, notice_no, notice_start_date, notice_end_date, " +
				"process_state, sex, neutered_state, special_mark, species, animal_location_id, breed_type_id, created_at, updated_at) VALUES ");

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

	private void insertLargeTestS3Profiles(int count) {
		StringBuilder sql = new StringBuilder(
			"INSERT INTO animal_s3_profile (animal_s3_profile_id, profile, image_type, is_deleted, animal_id, created_at, updated_at) VALUES ");

		for (int i = 1; i <= count; i++) {
			if (i > 1) {
				sql.append(",");
			}
			sql.append(String.format("(%d, 'animal-profiles/test-%d.webp', 'WEBP', true, %d, NOW(), NOW())", i, i, i));
		}

		namedParameterJdbcTemplate.getJdbcTemplate().execute(sql.toString());
	}

	private void cleanupJobExecutions() {
		jobRepositoryTestUtils.removeJobExecutions();
	}

	private void setupTestData() {
		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO animal_location (animal_location_id, name, address, center_no, coordinates, created_at, updated_at) " +
				"VALUES (1, '테스트 보호소', '서울시 강남구', 'TEST-001', ST_GeomFromText('POINT(37.0 127.0)', 4326), NOW(), NOW())");

		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO breed_type (breed_type_id, code, name, type, created_at, updated_at) " +
				"VALUES (1, '417000', '믹스견', 'DOG', NOW(), NOW())");

		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO animal (animal_id, desertion_no, happen_date, happen_place, " +
				"color, birth, weight, notice_no, notice_start_date, notice_end_date, " +
				"process_state, sex, neutered_state, special_mark, species, " +
				"animal_location_id, breed_type_id, created_at, updated_at) VALUES " +
				"(1, 'TEST-001', '2025-01-01', '서울시 강남구', '갈색', '2020(년생)', '5(Kg)', 'NOTICE-001', " +
				"'2025-01-01', '2025-01-15', '보호중', 'M', 'Y', '순함', 'DOG', 1, 1, NOW(), NOW()), " +
				"(2, 'TEST-002', '2025-01-02', '서울시 송파구', '흰색', '2021(년생)', '4(Kg)', 'NOTICE-002', " +
				"'2025-01-02', '2025-01-16', '보호중', 'F', 'N', '활발', 'DOG', 1, 1, NOW(), NOW()), " +
				"(3, 'TEST-003', '2025-01-03', '서울시 관악구', '검은색', '2022(년생)', '3(Kg)', 'NOTICE-003', " +
				"'2025-01-03', '2025-01-17', '보호중', 'M', 'Y', '조용함', 'DOG', 1, 1, NOW(), NOW())");

		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO animal_s3_profile (animal_s3_profile_id, profile, image_type, is_deleted, animal_id, created_at, updated_at) VALUES " +
				"(1, 'animal-profiles/test1.webp', 'WEBP', true, 1, NOW(), NOW()), " +
				"(2, 'animal-profiles/test2.webp', 'WEBP', true, 2, NOW(), NOW()), " +
				"(3, 'animal-profiles/test3.webp', 'WEBP', true, 3, NOW(), NOW())");
	}

	private int countDeletedS3Profiles() {
		return namedParameterJdbcTemplate.getJdbcTemplate()
			.queryForObject("SELECT COUNT(*) FROM animal_s3_profile WHERE is_deleted = true", Integer.class);
	}

	private int countAllS3Profiles() {
		return namedParameterJdbcTemplate.getJdbcTemplate()
			.queryForObject("SELECT COUNT(*) FROM animal_s3_profile", Integer.class);
	}
}
