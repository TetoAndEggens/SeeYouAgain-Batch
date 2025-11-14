package tetoandeggens.seeyouagainbatch.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnimalEntityField {

    ANIMAL_TYPE("animal_type"),
    DESERTION_NO("desertion_no"),
    HAPPEN_DATE("happen_date"),
    HAPPEN_PLACE("happen_place"),
    CITY("city"),
    TOWN("town"),
    SPECIES("species"),
    COLOR("color"),
    BIRTH("birth"),
    WEIGHT("weight"),
    NOTICE_NO("notice_no"),
    NOTICE_START_DATE("notice_start_date"),
    NOTICE_END_DATE("notice_end_date"),
    PROCESS_STATE("process_state"),
    SEX("sex"),
    NEUTERED_STATE("neutered_state"),
    SPECIAL_MARK("special_mark"),
    CENTER_PHONE("center_phone"),
    FINAL_UPDATED_AT("final_updated_at"),
    ANIMAL_LOCATION_ID("animal_location_id"),
    BREED_TYPE_ID("breed_type_id"),
    CREATED_AT("created_at"),
    UPDATED_AT("updated_at");

    private final String columnName;
}