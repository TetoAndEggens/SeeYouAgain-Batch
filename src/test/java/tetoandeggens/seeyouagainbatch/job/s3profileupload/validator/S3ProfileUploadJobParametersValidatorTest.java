package tetoandeggens.seeyouagainbatch.job.s3profileupload.validator;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;

@DisplayName("S3ProfileUploadJobParametersValidator 단위 테스트")
class S3ProfileUploadJobParametersValidatorTest {

	private S3ProfileUploadJobParametersValidator validator;

	@BeforeEach
	void setUp() {
		validator = new S3ProfileUploadJobParametersValidator();
	}

	@Test
	@DisplayName("유효한 파라미터로 검증 시 성공해야 한다")
	void shouldPassValidationWithValidParameters() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("startDate", "20250101")
			.addString("endDate", "20250131")
			.toJobParameters();

		assertThatCode(() -> validator.validate(parameters))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("null 파라미터로 검증 시 예외가 발생해야 한다")
	void shouldThrowExceptionWhenParametersIsNull() {
		assertThatThrownBy(() -> validator.validate(null))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("Job 파라미터가 없습니다");
	}

	@Test
	@DisplayName("startDate가 없으면 예외가 발생해야 한다")
	void shouldThrowExceptionWhenStartDateIsMissing() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("endDate", "20250131")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("startDate")
			.hasMessageContaining("필수값");
	}

	@Test
	@DisplayName("endDate가 없으면 예외가 발생해야 한다")
	void shouldThrowExceptionWhenEndDateIsMissing() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("startDate", "20250101")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("endDate")
			.hasMessageContaining("필수값");
	}

	@Test
	@DisplayName("잘못된 날짜 형식이면 예외가 발생해야 한다")
	void shouldThrowExceptionWhenDateFormatIsInvalid() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("startDate", "2025-01-01")
			.addString("endDate", "20250131")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("날짜 형식이 올바르지 않습니다");
	}

	@Test
	@DisplayName("시작일이 종료일보다 늦으면 예외가 발생해야 한다")
	void shouldThrowExceptionWhenStartDateIsAfterEndDate() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("startDate", "20250131")
			.addString("endDate", "20250101")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("시작일이 종료일보다 늦을 수 없습니다");
	}

	@Test
	@DisplayName("시작일과 종료일이 같으면 통과해야 한다")
	void shouldPassValidationWhenStartDateEqualsEndDate() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("startDate", "20250101")
			.addString("endDate", "20250101")
			.toJobParameters();

		assertThatCode(() -> validator.validate(parameters))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("빈 문자열 startDate는 예외가 발생해야 한다")
	void shouldThrowExceptionWhenStartDateIsEmpty() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("startDate", "")
			.addString("endDate", "20250131")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("startDate")
			.hasMessageContaining("필수값");
	}

	@Test
	@DisplayName("공백 문자열 endDate는 예외가 발생해야 한다")
	void shouldThrowExceptionWhenEndDateIsBlank() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("startDate", "20250101")
			.addString("endDate", "   ")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("endDate")
			.hasMessageContaining("필수값");
	}

	@Test
	@DisplayName("잘못된 월 형식(13월)이면 예외가 발생해야 한다")
	void shouldThrowExceptionWhenMonthIsInvalid() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("startDate", "20251301")
			.addString("endDate", "20251231")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("날짜 형식이 올바르지 않습니다");
	}

	@Test
	@DisplayName("잘못된 일 형식(32일)이면 예외가 발생해야 한다")
	void shouldThrowExceptionWhenDayIsInvalid() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("startDate", "20250101")
			.addString("endDate", "20250132")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("날짜 형식이 올바르지 않습니다");
	}

	@Test
	@DisplayName("짧은 날짜 형식이면 예외가 발생해야 한다")
	void shouldThrowExceptionWhenDateFormatIsTooShort() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("startDate", "2025011")
			.addString("endDate", "20250131")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("날짜 형식이 올바르지 않습니다");
	}

	@Test
	@DisplayName("긴 날짜 형식이면 예외가 발생해야 한다")
	void shouldThrowExceptionWhenDateFormatIsTooLong() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("startDate", "202501011")
			.addString("endDate", "20250131")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("날짜 형식이 올바르지 않습니다");
	}

	@Test
	@DisplayName("알파벳이 포함된 날짜는 예외가 발생해야 한다")
	void shouldThrowExceptionWhenDateContainsLetters() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("startDate", "2025Jan01")
			.addString("endDate", "20250131")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("날짜 형식이 올바르지 않습니다");
	}
}