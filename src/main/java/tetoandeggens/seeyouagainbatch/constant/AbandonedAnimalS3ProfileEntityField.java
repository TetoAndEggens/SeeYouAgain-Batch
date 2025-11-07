package tetoandeggens.seeyouagainbatch.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AbandonedAnimalS3ProfileEntityField {

	PROFILE("profile"),
	IMAGE_TYPE("image_type"),
	ABANDONED_ANIMAL_ID("abandoned_animal_id");

	private final String columnName;
}