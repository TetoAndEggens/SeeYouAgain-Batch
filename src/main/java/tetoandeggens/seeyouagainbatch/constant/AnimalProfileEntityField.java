package tetoandeggens.seeyouagainbatch.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnimalProfileEntityField {

	PROFILE("profile"),
	HAPPEN_DATE("happen_date"),
	ANIMAL_ID("animal_id"),
	CREATED_AT("created_at"),
	UPDATED_AT("updated_at");

	private final String columnName;
}