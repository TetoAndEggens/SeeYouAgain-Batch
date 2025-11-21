package tetoandeggens.seeyouagainbatch.job.s3profiledelete.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum S3ProfileDeleteJobParameterKey {

	START_DATE("startDate"),
	END_DATE("endDate");

	private final String key;
}