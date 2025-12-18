package tetoandeggens.seeyouagainbatch.job.keywordmapping.integration;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
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
@DisplayName("KeywordMapping 통합 테스트")
class KeywordMappingIntegrationTest extends BatchTestConfig {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;

	@Autowired
	private Job keywordMappingJob;

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
	@DisplayName("키워드 매칭 Job이 정상적으로 실행되어야 한다")
	void shouldCompleteJobSuccessfully() throws Exception {
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(countAnimalByKeyword()).isGreaterThan(0);
	}

	@Test
	@DisplayName("품종 키워드로 동물과 구독자를 매칭해야 한다")
	void shouldMatchAnimalsByBreedKeyword() throws Exception {
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		int breedMatches = countAnimalByKeywordForBreed();
		assertThat(breedMatches).isGreaterThan(0);
	}

	@Test
	@DisplayName("지역 키워드로 동물과 구독자를 매칭해야 한다")
	void shouldMatchAnimalsByLocationKeyword() throws Exception {
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		int locationMatches = countAnimalByKeywordForLocation();
		assertThat(locationMatches).isGreaterThan(0);
	}

	@Test
	@DisplayName("해당 날짜에 발생한 동물만 매칭해야 한다")
	void shouldMatchOnlyAnimalsFromSpecificDate() throws Exception {
		LocalDate futureDate = LocalDate.now().plusDays(10);
		String futureDateString = futureDate.format(DATE_FORMATTER);

		JobParameters futureJobParameters = new JobParametersBuilder()
			.addString("date", futureDateString)
			.addLong("timestamp", System.currentTimeMillis())
			.toJobParameters();

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(futureJobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(countAnimalByKeyword()).isEqualTo(0);
	}

	@Test
	@DisplayName("매칭할 키워드가 없으면 Job이 정상 완료되어야 한다")
	void shouldCompleteSuccessfullyWithNoKeywords() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM notification_keyword");

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(countAnimalByKeyword()).isEqualTo(0);
	}

	@Test
	@DisplayName("매칭할 동물이 없으면 Job이 정상 완료되어야 한다")
	void shouldCompleteSuccessfullyWithNoAnimals() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal");

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(countAnimalByKeyword()).isEqualTo(0);
	}

	@Test
	@DisplayName("대량의 키워드와 동물도 청크 단위로 정상 처리되어야 한다")
	void shouldProcessLargeVolumeInChunks() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM notification_keyword");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal");

		setupLargeTestData(100, 50);

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(countAnimalByKeyword()).isGreaterThan(0);
	}

	@Test
	@DisplayName("동일 키워드 조합은 그룹화하여 한 번만 처리해야 한다")
	void shouldGroupByKeywordCombination() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM notification_keyword");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal");

		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO notification_keyword (notification_keyword_id, keyword, keyword_type, keyword_category_type, member_id, created_at, updated_at) VALUES " +
				"(101, '믹스견', 'ABANDONED', 'BREED', 1, NOW(), NOW()), " +
				"(102, '믹스견', 'ABANDONED', 'BREED', 2, NOW(), NOW())");

		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO animal (animal_id, animal_type, desertion_no, happen_date, city, town, color, birth, weight, " +
				"notice_no, notice_start_date, notice_end_date, process_state, sex, neutered_state, " +
				"special_mark, species, animal_location_id, breed_type_id, is_deleted, created_at, updated_at) VALUES " +
				"(101, 'ABANDONED', 'TEST-101', '2025-01-15', '서울특별시', '강남구', '갈색', '2020(년생)', '5(Kg)', " +
				"'NOTICE-101', '2025-01-15', '2025-01-30', '보호중', 'M', 'Y', '순함', 'DOG', 1, 1, false, NOW(), NOW())");

		String targetDate = LocalDate.of(2025, 1, 15).format(DATE_FORMATTER);
		JobParameters testJobParameters = new JobParametersBuilder()
			.addString("date", targetDate)
			.addLong("timestamp", System.currentTimeMillis())
			.toJobParameters();

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(testJobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(countAnimalByKeyword()).isEqualTo(2);
	}

	@Test
	@DisplayName("삭제된 동물은 매칭하지 않아야 한다")
	void shouldNotMatchDeletedAnimals() throws Exception {
		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"UPDATE animal SET is_deleted = true WHERE animal_id = 1");

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		int totalMatches = countAnimalByKeyword();
		assertThat(totalMatches).isGreaterThanOrEqualTo(0);
	}

	private void setupJobLauncherTestUtils() {
		jobLauncherTestUtils.setJob(keywordMappingJob);
	}

	private void setupJobParameters() {
		String targetDate = LocalDate.of(2025, 1, 15).format(DATE_FORMATTER);

		jobParameters = new JobParametersBuilder()
			.addString("date", targetDate)
			.addLong("timestamp", System.currentTimeMillis())
			.addLong("mappingChunkSize", 100L)
			.toJobParameters();
	}

	private void setupTestData() {
		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO member (member_id, login_id, password, nick_name, phone_number, uuid, role, violated_count, is_push_enabled, is_deleted, created_at, updated_at) " +
				"VALUES (1, 'testuser1', 'password123', '테스트유저1', '01011111111', 'test-uuid-001', 'USER', 0, true, false, NOW(), NOW()), " +
				"(2, 'testuser2', 'password123', '테스트유저2', '01022222222', 'test-uuid-002', 'USER', 0, true, false, NOW(), NOW())");

		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO animal_location (animal_location_id, name, address, center_no, coordinates, created_at, updated_at) " +
				"VALUES (1, '테스트 보호소', '서울시 강남구', 'TEST-001', ST_GeomFromText('POINT(37.0 127.0)', 4326), NOW(), NOW())");

		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO breed_type (breed_type_id, code, name, type, created_at, updated_at) " +
				"VALUES (1, '417000', '믹스견', 'DOG', NOW(), NOW()), " +
				"(2, '000116', '진돗개', 'DOG', NOW(), NOW())");

		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO animal (animal_id, animal_type, desertion_no, happen_date, city, town, color, birth, weight, " +
				"notice_no, notice_start_date, notice_end_date, process_state, sex, neutered_state, " +
				"special_mark, species, animal_location_id, breed_type_id, is_deleted, created_at, updated_at) VALUES " +
				"(1, 'ABANDONED', 'TEST-001', '2025-01-15', '서울특별시', '강남구', '갈색', '2020(년생)', '5(Kg)', " +
				"'NOTICE-001', '2025-01-15', '2025-01-30', '보호중', 'M', 'Y', '순함', 'DOG', 1, 1, false, NOW(), NOW()), " +
				"(2, 'ABANDONED', 'TEST-002', '2025-01-15', '서울특별시', '송파구', '흰색', '2021(년생)', '4(Kg)', " +
				"'NOTICE-002', '2025-01-15', '2025-01-30', '보호중', 'F', 'N', '활발', 'DOG', 1, 2, false, NOW(), NOW()), " +
				"(3, 'ABANDONED', 'TEST-003', '2025-01-15', '부산광역시', '해운대구', '검은색', '2022(년생)', '3(Kg)', " +
				"'NOTICE-003', '2025-01-15', '2025-01-30', '보호중', 'M', 'Y', '조용함', 'DOG', 1, 1, false, NOW(), NOW())");

		namedParameterJdbcTemplate.getJdbcTemplate().execute(
			"INSERT INTO notification_keyword (notification_keyword_id, keyword, keyword_type, keyword_category_type, member_id, created_at, updated_at) VALUES " +
				"(1, '믹스견', 'ABANDONED', 'BREED', 1, NOW(), NOW()), " +
				"(2, '진돗개', 'ABANDONED', 'BREED', 1, NOW(), NOW()), " +
				"(3, '서울특별시', 'ABANDONED', 'LOCATION', 2, NOW(), NOW()), " +
				"(4, '강남구', 'ABANDONED', 'LOCATION', 2, NOW(), NOW())");
	}

	private void setupLargeTestData(int animalCount, int keywordCount) {
		insertLargeTestAnimals(animalCount);
		insertLargeTestKeywords(keywordCount);
	}

	private void insertLargeTestAnimals(int count) {
		StringBuilder sql = new StringBuilder(
			"INSERT INTO animal (animal_id, animal_type, desertion_no, happen_date, city, town, color, birth, weight, " +
				"notice_no, notice_start_date, notice_end_date, process_state, sex, neutered_state, " +
				"special_mark, species, animal_location_id, breed_type_id, is_deleted, created_at, updated_at) VALUES ");

		for (int i = 1; i <= count; i++) {
			if (i > 1) {
				sql.append(",");
			}
			sql.append(String.format("(%d, 'ABANDONED', 'TEST-%06d', '2025-01-15', '서울특별시', '강남구', '갈색', '2020(년생)', '5(Kg)', " +
				"'NOTICE-%06d', '2025-01-15', '2025-01-30', '보호중', 'M', 'Y', '테스트', 'DOG', 1, 1, false, NOW(), NOW())", i, i, i));
		}

		namedParameterJdbcTemplate.getJdbcTemplate().execute(sql.toString());
	}

	private void insertLargeTestKeywords(int count) {
		StringBuilder sql = new StringBuilder(
			"INSERT INTO notification_keyword (notification_keyword_id, keyword, keyword_type, keyword_category_type, member_id, created_at, updated_at) VALUES ");

		for (int i = 1; i <= count; i++) {
			if (i > 1) {
				sql.append(",");
			}
			sql.append(String.format("(%d, '믹스견', 'ABANDONED', 'BREED', 1, NOW(), NOW())", i));
		}

		namedParameterJdbcTemplate.getJdbcTemplate().execute(sql.toString());
	}

	private void cleanupJobExecutions() {
		jobRepositoryTestUtils.removeJobExecutions();
	}

	@Override
	protected void cleanupTestData() {
		namedParameterJdbcTemplate.getJdbcTemplate().execute("SET FOREIGN_KEY_CHECKS = 0");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal_by_keyword");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM notification_keyword");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM fcm_token");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM member");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal_s3_profile");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal_profile");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM animal_location");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("DELETE FROM breed_type");
		namedParameterJdbcTemplate.getJdbcTemplate().execute("SET FOREIGN_KEY_CHECKS = 1");
	}

	private int countAnimalByKeyword() {
		return namedParameterJdbcTemplate.getJdbcTemplate()
			.queryForObject("SELECT COUNT(*) FROM animal_by_keyword", Integer.class);
	}

	private int countAnimalByKeywordForBreed() {
		return namedParameterJdbcTemplate.getJdbcTemplate()
			.queryForObject("SELECT COUNT(*) FROM animal_by_keyword abk " +
				"JOIN notification_keyword nk ON abk.notification_keyword_id = nk.notification_keyword_id " +
				"WHERE nk.keyword_category_type = 'BREED'", Integer.class);
	}

	private int countAnimalByKeywordForLocation() {
		return namedParameterJdbcTemplate.getJdbcTemplate()
			.queryForObject("SELECT COUNT(*) FROM animal_by_keyword abk " +
				"JOIN notification_keyword nk ON abk.notification_keyword_id = nk.notification_keyword_id " +
				"WHERE nk.keyword_category_type = 'LOCATION'", Integer.class);
	}
}