package tetoandeggens.seeyouagainbatch.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BreedTypeEntityField {

    CODE("code"),
    NAME("name"),
    TYPE("type"),
    CREATED_AT("created_at"),
    UPDATED_AT("updated_at");

    private final String columnName;
}