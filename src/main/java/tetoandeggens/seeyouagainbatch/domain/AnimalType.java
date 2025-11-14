package tetoandeggens.seeyouagainbatch.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnimalType {

    ABANDONED("유기"),
    MISSING("실종"),
    WITNESS("목격");

    private final String type;
}