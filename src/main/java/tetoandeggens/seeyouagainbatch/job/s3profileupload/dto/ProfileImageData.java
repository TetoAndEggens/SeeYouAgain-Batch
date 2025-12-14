package tetoandeggens.seeyouagainbatch.job.s3profileupload.dto;

import lombok.Builder;
import lombok.Getter;
import tetoandeggens.seeyouagainbatch.domain.AnimalProfile;

@Getter
@Builder
public class ProfileImageData {

	private final AnimalProfile profile;
	private final byte[] imageBytes;
	private final String s3Key;
}