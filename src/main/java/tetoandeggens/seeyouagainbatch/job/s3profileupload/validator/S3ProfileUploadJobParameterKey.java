package tetoandeggens.seeyouagainbatch.job.s3profileupload.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum S3ProfileUploadJobParameterKey {

	START_DATE("startDate"),
	END_DATE("endDate");

	private final String key;
}