package tetoandeggens.seeyouagainbatch.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KeywordCategoryType {

	BREED("품종"),
	LOCATION("지역");

	private final String description;
}