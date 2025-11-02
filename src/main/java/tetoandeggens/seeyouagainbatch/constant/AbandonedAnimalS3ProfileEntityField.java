package tetoandeggens.seeyouagainbatch.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AbandonedAnimalS3ProfileEntityField {

	OBJECT_KEY("object_key"),
	IMAGE_TYPE("image_type"),
	ABANDONED_ANIMAL_ID("abandoned_animal_id"),
	CREATED_AT("created_at"),
	UPDATED_AT("updated_at");

	private final String columnName;
}