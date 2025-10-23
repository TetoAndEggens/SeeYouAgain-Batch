package tetoandeggens.seeyouagainbatch.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Species {
	DOG("417000", "개"),
	CAT("422400", "고양이"),
	ETC("429900", "기타");

	private final String code;
	private final String type;
}