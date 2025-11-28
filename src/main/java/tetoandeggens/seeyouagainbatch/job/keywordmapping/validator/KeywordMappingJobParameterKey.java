package tetoandeggens.seeyouagainbatch.job.keywordmapping.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KeywordMappingJobParameterKey {

	DATE("date");

	private final String key;
}