package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AbandonedAnimalDataLoadValidationErrorMessage {

    PARAMETERS_NULL("Job 파라미터가 없습니다"),
    PARAMETER_REQUIRED("%s 파라미터는 필수값입니다"),
    PARAMETER_OUT_OF_RANGE("%s 값이 허용 범위를 벗어났습니다: %d (허용 범위: %d ~ %d)"),
    INVALID_DATE_FORMAT("%s 날짜 형식이 올바르지 않습니다: %s (올바른 형식: yyyy-MM-dd)"),
    START_DATE_AFTER_END_DATE("시작일이 종료일보다 늦을 수 없습니다. 시작일: %s, 종료일: %s");

    private final String messageFormat;

    public String format(Object... args) {
        return String.format(messageFormat, args);
    }
}