package tetoandeggens.seeyouagainbatch.job.keywordnotification.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KeywordNotificationJobParameterKey {

	DATE("date");

	private final String key;
}