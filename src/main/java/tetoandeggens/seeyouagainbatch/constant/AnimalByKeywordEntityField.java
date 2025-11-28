package tetoandeggens.seeyouagainbatch.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnimalByKeywordEntityField {

	NOTIFICATION_KEYWORD_ID("notification_keyword_id"),
	ANIMAL_ID("animal_id"),
	CREATED_AT("created_at"),
	UPDATED_AT("updated_at");

	private final String columnName;
}
