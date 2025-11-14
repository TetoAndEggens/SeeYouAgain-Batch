package tetoandeggens.seeyouagainbatch.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnimalS3ProfileEntityField {

	PROFILE("profile"),
	IMAGE_TYPE("image_type"),
	ANIMAL_ID("animal_id");

	private final String columnName;
}