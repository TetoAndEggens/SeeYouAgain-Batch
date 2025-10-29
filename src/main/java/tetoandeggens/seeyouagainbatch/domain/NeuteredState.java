package tetoandeggens.seeyouagainbatch.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NeuteredState {
	Y("Y", "중성화"),
	N("N", "비중성화"),
	U("U", "미상");

	private final String code;
	private final String type;

	public static NeuteredState fromCode(String code) {
		if (code == null) {
			return U;
		}
		for (NeuteredState state : values()) {
			if (state.code.equalsIgnoreCase(code)) {
				return state;
			}
		}
		return U;
	}
}