package tetoandeggens.seeyouagainbatch.job.fcmtokendelete.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FcmTokenDeleteValidationErrorMessage {

	PARAMETERS_NULL("Job 파라미터가 없습니다"),
	PARAMETER_REQUIRED("%s 파라미터는 필수값입니다"),
	INVALID_DATE_FORMAT("%s 날짜 형식이 올바르지 않습니다: %s (올바른 형식: yyyyMMdd)"),
	DATE_NOT_OLD_ENOUGH("삭제 대상 날짜는 현재로부터 최소 %d일 이전이어야 합니다. 입력된 날짜: %s");

	private final String messageFormat;

	public String format(Object... args) {
		return String.format(messageFormat, args);
	}
}