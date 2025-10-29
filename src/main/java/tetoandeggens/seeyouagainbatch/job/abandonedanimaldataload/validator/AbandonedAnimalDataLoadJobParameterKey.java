package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AbandonedAnimalDataLoadJobParameterKey {

    START_DATE("startDate"),
    END_DATE("endDate"),
    PAGE_NO("pageNo"),
    NUM_OF_ROWS("numOfRows");

    private final String key;
}