package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.validator;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;

@DisplayName("AbandonedAnimalDataLoadJobParametersValidator 단위 테스트")
class AbandonedAnimalDataLoadJobParametersValidatorTest {

    private AbandonedAnimalDataLoadJobParametersValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AbandonedAnimalDataLoadJobParametersValidator();
    }

    @Test
    @DisplayName("유효한 파라미터로 검증 시 성공해야 한다")
    void shouldPassValidationWithValidParameters() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("startDate", "20250101")
                .addString("endDate", "20250131")
                .addLong("pageNo", 1L)
                .addLong("numOfRows", 100L)
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
                .addLong("pageNo", 1L)
                .addLong("numOfRows", 100L)
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
                .addLong("pageNo", 1L)
                .addLong("numOfRows", 100L)
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(parameters))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("endDate")
                .hasMessageContaining("필수값");
    }

    @Test
    @DisplayName("pageNo가 없으면 예외가 발생해야 한다")
    void shouldThrowExceptionWhenPageNoIsMissing() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("startDate", "20250101")
                .addString("endDate", "20250131")
                .addLong("numOfRows", 100L)
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(parameters))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("pageNo")
                .hasMessageContaining("필수값");
    }

    @Test
    @DisplayName("numOfRows가 없으면 예외가 발생해야 한다")
    void shouldThrowExceptionWhenNumOfRowsIsMissing() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("startDate", "20250101")
                .addString("endDate", "20250131")
                .addLong("pageNo", 1L)
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(parameters))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("numOfRows")
                .hasMessageContaining("필수값");
    }

    @Test
    @DisplayName("잘못된 날짜 형식이면 예외가 발생해야 한다")
    void shouldThrowExceptionWhenDateFormatIsInvalid() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("startDate", "2025-01-01")
                .addString("endDate", "20250131")
                .addLong("pageNo", 1L)
                .addLong("numOfRows", 100L)
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
                .addLong("pageNo", 1L)
                .addLong("numOfRows", 100L)
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(parameters))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("시작일이 종료일보다 늦을 수 없습니다");
    }

    @Test
    @DisplayName("pageNo가 최소값보다 작으면 예외가 발생해야 한다")
    void shouldThrowExceptionWhenPageNoIsLessThanMin() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("startDate", "20250101")
                .addString("endDate", "20250131")
                .addLong("pageNo", 0L)
                .addLong("numOfRows", 100L)
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(parameters))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("pageNo")
                .hasMessageContaining("허용 범위를 벗어났습니다");
    }

    @Test
    @DisplayName("numOfRows가 최소값보다 작으면 예외가 발생해야 한다")
    void shouldThrowExceptionWhenNumOfRowsIsLessThanMin() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("startDate", "20250101")
                .addString("endDate", "20250131")
                .addLong("pageNo", 1L)
                .addLong("numOfRows", 0L)
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(parameters))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("numOfRows")
                .hasMessageContaining("허용 범위를 벗어났습니다");
    }

    @Test
    @DisplayName("numOfRows가 최대값보다 크면 예외가 발생해야 한다")
    void shouldThrowExceptionWhenNumOfRowsIsGreaterThanMax() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("startDate", "20250101")
                .addString("endDate", "20250131")
                .addLong("pageNo", 1L)
                .addLong("numOfRows", 10001L)
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(parameters))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("numOfRows")
                .hasMessageContaining("허용 범위를 벗어났습니다");
    }

    @Test
    @DisplayName("경계값 테스트 - pageNo 최소값")
    void shouldPassValidationWithMinPageNo() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("startDate", "20250101")
                .addString("endDate", "20250131")
                .addLong("pageNo", 1L)
                .addLong("numOfRows", 100L)
                .toJobParameters();

        assertThatCode(() -> validator.validate(parameters))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("경계값 테스트 - numOfRows 최소값")
    void shouldPassValidationWithMinNumOfRows() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("startDate", "20250101")
                .addString("endDate", "20250131")
                .addLong("pageNo", 1L)
                .addLong("numOfRows", 1L)
                .toJobParameters();

        assertThatCode(() -> validator.validate(parameters))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("경계값 테스트 - numOfRows 최대값")
    void shouldPassValidationWithMaxNumOfRows() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("startDate", "20250101")
                .addString("endDate", "20250131")
                .addLong("pageNo", 1L)
                .addLong("numOfRows", 10000L)
                .toJobParameters();

        assertThatCode(() -> validator.validate(parameters))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("시작일과 종료일이 같으면 통과해야 한다")
    void shouldPassValidationWhenStartDateEqualsEndDate() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("startDate", "20250101")
                .addString("endDate", "20250101")
                .addLong("pageNo", 1L)
                .addLong("numOfRows", 100L)
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
                .addLong("pageNo", 1L)
                .addLong("numOfRows", 100L)
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
                .addLong("pageNo", 1L)
                .addLong("numOfRows", 100L)
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(parameters))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("endDate")
                .hasMessageContaining("필수값");
    }
}