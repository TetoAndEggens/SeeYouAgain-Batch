package tetoandeggens.seeyouagainbatch.job.animaldataload.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

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

import tetoandeggens.seeyouagainbatch.config.BatchIntegrationTest;
import tetoandeggens.seeyouagainbatch.config.BatchTestConfig;
import tetoandeggens.seeyouagainbatch.job.animaldataload.client.KakaoMapApiClient;
import tetoandeggens.seeyouagainbatch.job.animaldataload.client.PublicDataApiClient;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.AnimalApiResponseWrapper;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.AnimalPublicDataDto;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.KakaoMapDocumentDto;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.KakaoMapResponseWrapper;

@BatchIntegrationTest
class AnimalDataLoadIntegrationTest extends BatchTestConfig {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;

	@Autowired
	private Job animalDataLoadJob;

	@MockitoBean
	private PublicDataApiClient publicDataApiClient;

	@MockitoBean
	private KakaoMapApiClient kakaoMapApiClient;

	private JobParameters jobParameters;

	@BeforeEach
	void setUp() {
		setupJobLauncherTestUtils();
		setupMockApis();
		setupJobParameters();
	}

	@AfterEach
	void tearDown() {
		cleanupJobExecutions();
		cleanupTestData();
	}

	@Test
	@DisplayName("동물 데이터 로드 Job이 정상적으로 실행되어야 한다")
	void shouldCompleteJobSuccessfullyWithValidData() throws Exception {
		when(publicDataApiClient.fetchAnimals(
			anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt()))
			.thenReturn(createMockApiResponse())
			.thenReturn(createEmptyApiResponse());

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(countCenterLocations()).isEqualTo(2);
		assertThat(countBreedTypes()).isEqualTo(2);
		assertThat(countAnimals()).isEqualTo(2);
		assertThat(countAnimalProfiles()).isEqualTo(6);

		verify(publicDataApiClient, atLeastOnce()).fetchAnimals(
			anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt());
		verify(kakaoMapApiClient, atLeast(2)).searchCoordinates(anyString(), anyString());
	}

	@Test
	@DisplayName("빈 API 응답 시 Job이 정상 완료되어야 한다")
	void shouldCompleteSuccessfullyWithEmptyApiResponse() throws Exception {
		when(publicDataApiClient.fetchAnimals(
			anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt()))
			.thenReturn(createEmptyApiResponse());

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(countAnimals()).isEqualTo(0);
		assertThat(countCenterLocations()).isEqualTo(0);
		assertThat(countBreedTypes()).isEqualTo(0);
	}

	@Test
	@DisplayName("여러 페이지 데이터를 순차적으로 처리해야 한다")
	void shouldProcessMultiplePagesSequentially() throws Exception {
		when(publicDataApiClient.fetchAnimals(
			anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt()))
			.thenReturn(createMockApiResponse())
			.thenReturn(createSecondPageMockResponse())
			.thenReturn(createEmptyApiResponse());

		JobParameters multiPageJobParameters = jobLauncherTestUtils.getUniqueJobParametersBuilder()
			.addString("startDate", "20250101")
			.addString("endDate", "20250131")
			.addLong("numOfRows", 2L)
			.addLong("pageNo", 1L)
			.toJobParameters();

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(multiPageJobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(countAnimals()).isEqualTo(4);

		verify(publicDataApiClient, times(3)).fetchAnimals(
			anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt());
	}

	@Test
	@DisplayName("동일한 보호소의 동물이 여러 건 있어도 보호소는 한 번만 저장되어야 한다")
	void shouldSaveCenterLocationOnlyOnce() throws Exception {
		when(publicDataApiClient.fetchAnimals(
			anyString(), anyString(), anyString(), anyInt(), anyString(), anyInt()))
			.thenReturn(createMockApiResponseWithSameCenterLocation())
			.thenReturn(createEmptyApiResponse());

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(countCenterLocations()).isEqualTo(1);

		assertThat(countAnimals()).isGreaterThan(1);
	}

	private void setupJobLauncherTestUtils() {
		jobLauncherTestUtils.setJob(animalDataLoadJob);
	}

	private void setupMockApis() {
		KakaoMapDocumentDto document = new KakaoMapDocumentDto(
			String.valueOf(126.9780),
			String.valueOf(37.5665)
		);

		KakaoMapResponseWrapper wrapper = new KakaoMapResponseWrapper(List.of(document));

		when(kakaoMapApiClient.searchCoordinates(anyString(), anyString()))
			.thenReturn(wrapper);
	}

	private void setupJobParameters() {
		jobParameters = jobLauncherTestUtils.getUniqueJobParametersBuilder()
			.addString("startDate", "20250101")
			.addString("endDate", "20250131")
			.addLong("numOfRows", 100L)
			.addLong("pageNo", 1L)
			.toJobParameters();
	}

	private void cleanupJobExecutions() {
		jobRepositoryTestUtils.removeJobExecutions();
	}

	private int countCenterLocations() {
		return namedParameterJdbcTemplate.getJdbcTemplate()
			.queryForObject("SELECT COUNT(*) FROM animal_location", Integer.class);
	}

	private int countBreedTypes() {
		return namedParameterJdbcTemplate.getJdbcTemplate()
			.queryForObject("SELECT COUNT(*) FROM breed_type", Integer.class);
	}

	private int countAnimals() {
		return namedParameterJdbcTemplate.getJdbcTemplate()
			.queryForObject("SELECT COUNT(*) FROM animal", Integer.class);
	}

	private int countAnimalProfiles() {
		return namedParameterJdbcTemplate.getJdbcTemplate()
			.queryForObject("SELECT COUNT(*) FROM animal_profile", Integer.class);
	}

	private AnimalApiResponseWrapper createMockApiResponse() {
		List<AnimalPublicDataDto> itemList = List.of(
			createTestAnimalDto("448853202500001", "20250101", "서울특별시 강남구",
				"[개] 믹스견", "417000", "개", "믹스견", "갈색", "2023(년생)", "5(Kg)",
				"서울특별시 강남구", "강남구청", "02-3423-5555",
				"서울특별시 강남구 학동로 426", "test-reg-001", "종료(입양)", "M", "Y",
				"매우 순하고 사람을 좋아함"),
			createTestAnimalDto("448853202500002", "20250102", "서울특별시 송파구",
				"[고양이] 코리안숏헤어", "422400", "고양이", "코리안숏헤어", "검은색", "2024(년생)", "3.5(Kg)",
				"서울특별시 송파구", "송파구청", "02-2147-2222",
				"서울특별시 송파구 올림픽로 326", "test-reg-002", "보호중", "F", "N",
				"건강 상태 양호, 사람에게 친근함")
		);

		AnimalApiResponseWrapper.Items items = new AnimalApiResponseWrapper.Items(itemList);
		AnimalApiResponseWrapper.Body body = new AnimalApiResponseWrapper.Body(items);
		AnimalApiResponseWrapper.Response response = new AnimalApiResponseWrapper.Response(body);
		return new AnimalApiResponseWrapper(response);
	}

	private AnimalApiResponseWrapper createSecondPageMockResponse() {
		List<AnimalPublicDataDto> itemList = List.of(
			createTestAnimalDto("448853202500003", "20250103", "경기도 성남시",
				"[개] 포메라니안", "410000", "개", "포메라니안", "흰색", "2022(년생)", "3(Kg)",
				"경기도 성남시", "성남시청", "031-729-3333",
				"경기도 성남시 중원구 성남대로 997", "test-reg-003", "공고중", "M", "Y",
				"활발하고 건강함"),
			createTestAnimalDto("448853202500004", "20250104", "인천광역시 남동구",
				"[개] 리트리버", "417000", "개", "리트리버", "황금색", "2021(년생)", "25(Kg)",
				"인천광역시 남동구", "남동구청", "032-453-4444",
				"인천광역시 남동구 소래로 633", "test-reg-004", "보호중", "F", "N",
				"온순하고 사람을 잘 따름")
		);

		AnimalApiResponseWrapper.Items items = new AnimalApiResponseWrapper.Items(itemList);
		AnimalApiResponseWrapper.Body body = new AnimalApiResponseWrapper.Body(items);
		AnimalApiResponseWrapper.Response response = new AnimalApiResponseWrapper.Response(body);
		return new AnimalApiResponseWrapper(response);
	}

	private AnimalApiResponseWrapper createMockApiResponseWithSameCenterLocation() {
		List<AnimalPublicDataDto> itemList = List.of(
			createTestAnimalDto("448853202500011", "20250101", "서울특별시 강남구",
				"[개] 믹스견", "417000", "개", "믹스견", "갈색", "2023(년생)", "5(Kg)",
				"서울특별시 강남구", "강남구청", "02-3423-5555",
				"서울특별시 강남구 학동로 426", "test-reg-same", "보호중", "M", "Y",
				"순함"),
			createTestAnimalDto("448853202500012", "20250102", "서울특별시 강남구",
				"[개] 푸들", "417000", "개", "푸들", "흰색", "2022(년생)", "4(Kg)",
				"서울특별시 강남구", "강남구청", "02-3423-5555",
				"서울특별시 강남구 학동로 426", "test-reg-same", "보호중", "F", "Y",
				"활발함"),
			createTestAnimalDto("448853202500013", "20250103", "서울특별시 강남구",
				"[고양이] 코리안숏헤어", "422400", "고양이", "코리안숏헤어", "검은색", "2024(년생)", "3(Kg)",
				"서울특별시 강남구", "강남구청", "02-3423-5555",
				"서울특별시 강남구 학동로 426", "test-reg-same", "보호중", "M", "N",
				"조용함")
		);

		AnimalApiResponseWrapper.Items items = new AnimalApiResponseWrapper.Items(itemList);
		AnimalApiResponseWrapper.Body body = new AnimalApiResponseWrapper.Body(items);
		AnimalApiResponseWrapper.Response response = new AnimalApiResponseWrapper.Response(body);
		return new AnimalApiResponseWrapper(response);
	}

	private AnimalApiResponseWrapper createEmptyApiResponse() {
		AnimalApiResponseWrapper.Items items = new AnimalApiResponseWrapper.Items(List.of());
		AnimalApiResponseWrapper.Body body = new AnimalApiResponseWrapper.Body(items);
		AnimalApiResponseWrapper.Response response = new AnimalApiResponseWrapper.Response(body);
		return new AnimalApiResponseWrapper(response);
	}

	private AnimalPublicDataDto createTestAnimalDto(
		String desertionNo, String happenDt, String happenPlace,
		String kindFullNm, String kindCd, String upKindNm, String kindNm,
		String colorCd, String age, String weight,
		String orgNm, String careNm, String careTel, String careAddr, String careRegNo,
		String processState, String sexCd, String neuterYn, String specialMark) {

		return new AnimalPublicDataDto(
			desertionNo,
			happenDt,
			happenPlace,
			kindFullNm,
			upKindNm.equals("개") ? "417000" : "422400",
			upKindNm,
			kindNm,
			kindCd,
			colorCd,
			age,
			weight,
			"test-notice-" + desertionNo,
			happenDt,
			"20250228",
			"http://example.com/image1.jpg",
			"http://example.com/image2.jpg",
			"http://example.com/image3.jpg",
			processState,
			sexCd,
			neuterYn,
			specialMark,
			careNm,
			careTel,
			careAddr,
			careRegNo,
			"2025-01-15 10:30:00",
			orgNm
		);
	}
}