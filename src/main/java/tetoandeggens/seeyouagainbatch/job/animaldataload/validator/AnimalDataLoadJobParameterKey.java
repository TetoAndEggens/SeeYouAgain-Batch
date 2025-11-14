package tetoandeggens.seeyouagainbatch.job.animaldataload.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnimalDataLoadJobParameterKey {

    START_DATE("startDate"),
    END_DATE("endDate"),
    PAGE_NO("pageNo"),
    NUM_OF_ROWS("numOfRows");

    private final String key;
}