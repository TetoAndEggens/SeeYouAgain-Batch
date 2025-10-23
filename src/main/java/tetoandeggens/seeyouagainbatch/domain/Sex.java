package tetoandeggens.seeyouagainbatch.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Sex {
	M("M", "수컷"),
	F("F", "암컷"),
	Q("Q", "미상");

	private final String code;
	private final String type;
}