package tetoandeggens.seeyouagainbatch.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KeywordType {

	ABANDONED("유기"),
	WITNESS("목격");

	private final String description;
}