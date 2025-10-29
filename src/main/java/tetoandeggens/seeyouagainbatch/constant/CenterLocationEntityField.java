package tetoandeggens.seeyouagainbatch.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CenterLocationEntityField {

    NAME("name"),
    ADDRESS("address"),
    CENTER_NO("center_no"),
    LATITUDE("latitude"),
    LONGITUDE("longitude"),
    CREATED_AT("created_at"),
    UPDATED_AT("updated_at");

    private final String columnName;
}