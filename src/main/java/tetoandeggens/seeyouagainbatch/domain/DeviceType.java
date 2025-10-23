package tetoandeggens.seeyouagainbatch.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeviceType {
	WEB("WEB"),
	AOS("ANDROID"),
	IOS("APPLE");

	private final String type;
}