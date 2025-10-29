package tetoandeggens.seeyouagainbatch.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AbandonedAnimalProfileEntityField {

	POPFILE1("popfile1"),
	POPFILE2("popfile2"),
	POPFILE3("popfile3"),
	ABANDONED_ANIMAL_ID("abandoned_animal_id"),
	CREATED_AT("created_at"),
	UPDATED_AT("updated_at");

	private final String columnName;
}