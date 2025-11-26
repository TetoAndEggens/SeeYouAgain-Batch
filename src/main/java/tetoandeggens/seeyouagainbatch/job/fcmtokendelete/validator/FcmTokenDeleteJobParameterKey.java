package tetoandeggens.seeyouagainbatch.job.fcmtokendelete.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FcmTokenDeleteJobParameterKey {

	DATE("date");

	private final String key;
}