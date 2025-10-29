package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AbandonedAnimalDataLoadEntityField {

    // AbandonedAnimal fields
    DESERTION_NO("desertionNo"),
    HAPPEN_DATE("happenDate"),
    HAPPEN_PLACE("happenPlace"),
    SPECIES("species"),
    COLOR("color"),
    BIRTH("birth"),
    WEIGHT("weight"),
    NOTICE_NO("noticeNo"),
    NOTICE_START_DATE("noticeStartDate"),
    NOTICE_END_DATE("noticeEndDate"),
    PROCESS_STATE("processState"),
    SEX("sex"),
    NEUTERED_STATE("neuteredState"),
    SPECIAL_MARK("specialMark"),
    CENTER_PHONE("centerPhone"),
    FINAL_UPDATED_AT("finalUpdatedAt"),
    CENTER_LOCATION("centerLocation"),
    BREED_TYPE("breedType"),

    // CenterLocation fields
    NAME("name"),
    ADDRESS("address"),
    LATITUDE("latitude"),
    LONGITUDE("longitude"),
    CENTER_NO("centerNo"),

    // BreedType fields
    TYPE("type");

    private final String fieldName;
}