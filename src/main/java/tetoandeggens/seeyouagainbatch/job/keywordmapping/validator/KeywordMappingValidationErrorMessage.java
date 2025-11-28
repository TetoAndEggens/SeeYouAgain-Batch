package tetoandeggens.seeyouagainbatch.job.keywordmapping.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KeywordMappingValidationErrorMessage {

	PARAMETERS_NULL("Job 파라미터가 없습니다"),
	PARAMETER_REQUIRED("%s 파라미터는 필수값입니다"),
	INVALID_DATE_FORMAT("%s 날짜 형식이 올바르지 않습니다: %s (올바른 형식: yyyy-MM-dd)");

	private final String messageFormat;

	public String format(Object... args) {
		return String.format(messageFormat, args);
	}
}