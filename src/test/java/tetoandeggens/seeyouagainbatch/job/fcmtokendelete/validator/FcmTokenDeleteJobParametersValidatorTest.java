package tetoandeggens.seeyouagainbatch.job.fcmtokendelete.validator;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;

@DisplayName("FcmTokenDeleteJobParametersValidator 단위 테스트")
class FcmTokenDeleteJobParametersValidatorTest {

	private FcmTokenDeleteJobParametersValidator validator;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	@BeforeEach
	void setUp() {
		validator = new FcmTokenDeleteJobParametersValidator();
	}

	@Test
	@DisplayName("유효한 파라미터로 검증 시 성공해야 한다")
	void shouldPassValidationWithValidParameters() {
		String validDate = LocalDate.now().format(DATE_FORMATTER);

		JobParameters parameters = new JobParametersBuilder()
			.addString("date", validDate)
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
	@DisplayName("date가 없으면 예외가 발생해야 한다")
	void shouldThrowExceptionWhenDateIsMissing() {
		JobParameters parameters = new JobParametersBuilder()
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("date")
			.hasMessageContaining("필수값");
	}

	@Test
	@DisplayName("잘못된 날짜 형식이면 예외가 발생해야 한다")
	void shouldThrowExceptionWhenDateFormatIsInvalid() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("date", "2025-01-01")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("날짜 형식이 올바르지 않습니다");
	}


	@Test
	@DisplayName("빈 문자열 date는 예외가 발생해야 한다")
	void shouldThrowExceptionWhenDateIsEmpty() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("date", "")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("date")
			.hasMessageContaining("필수값");
	}

	@Test
	@DisplayName("공백 문자열 date는 예외가 발생해야 한다")
	void shouldThrowExceptionWhenDateIsBlank() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("date", "   ")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("date")
			.hasMessageContaining("필수값");
	}

	@Test
	@DisplayName("잘못된 월 형식(13월)이면 예외가 발생해야 한다")
	void shouldThrowExceptionWhenMonthIsInvalid() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("date", "20251301")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("날짜 형식이 올바르지 않습니다");
	}

	@Test
	@DisplayName("잘못된 일 형식(32일)이면 예외가 발생해야 한다")
	void shouldThrowExceptionWhenDayIsInvalid() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("date", "20250132")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("날짜 형식이 올바르지 않습니다");
	}

	@Test
	@DisplayName("짧은 날짜 형식이면 예외가 발생해야 한다")
	void shouldThrowExceptionWhenDateFormatIsTooShort() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("date", "2025011")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("날짜 형식이 올바르지 않습니다");
	}

	@Test
	@DisplayName("긴 날짜 형식이면 예외가 발생해야 한다")
	void shouldThrowExceptionWhenDateFormatIsTooLong() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("date", "202501011")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("날짜 형식이 올바르지 않습니다");
	}

	@Test
	@DisplayName("알파벳이 포함된 날짜는 예외가 발생해야 한다")
	void shouldThrowExceptionWhenDateContainsLetters() {
		JobParameters parameters = new JobParametersBuilder()
			.addString("date", "2025Jan01")
			.toJobParameters();

		assertThatThrownBy(() -> validator.validate(parameters))
			.isInstanceOf(JobParametersInvalidException.class)
			.hasMessageContaining("날짜 형식이 올바르지 않습니다");
	}

	@Test
	@DisplayName("과거 날짜도 통과해야 한다")
	void shouldPassValidationForPastDate() {
		String pastDate = LocalDate.now().minusDays(30).format(DATE_FORMATTER);

		JobParameters parameters = new JobParametersBuilder()
			.addString("date", pastDate)
			.toJobParameters();

		assertThatCode(() -> validator.validate(parameters))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("미래 날짜도 통과해야 한다")
	void shouldPassValidationForFutureDate() {
		String futureDate = LocalDate.now().plusDays(30).format(DATE_FORMATTER);

		JobParameters parameters = new JobParametersBuilder()
			.addString("date", futureDate)
			.toJobParameters();

		assertThatCode(() -> validator.validate(parameters))
			.doesNotThrowAnyException();
	}
}